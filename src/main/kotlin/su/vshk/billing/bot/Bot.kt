package su.vshk.billing.bot

import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.kotlin.core.publisher.toMono
import su.vshk.billing.bot.config.BotProperties
import su.vshk.billing.bot.dao.model.Command
import su.vshk.billing.bot.message.*
import su.vshk.billing.bot.message.dto.RequestMessageItem
import su.vshk.billing.bot.message.dto.ResponseMessageItem
import su.vshk.billing.bot.service.processor.ProcessorService
import su.vshk.billing.bot.service.processor.PostProcessorService
import su.vshk.billing.bot.util.*
import java.util.*

@Component
class Bot(
    private val properties: BotProperties,
    private val processorService: ProcessorService,
    private val postProcessorService: PostProcessorService
): TelegramLongPollingBot() {

    private val logger = getLogger()

    override fun getBotToken(): String =
        properties.token!!

    override fun getBotUsername(): String =
        properties.name!!

    /**
     * Точка входа для обработки сообщения от пользователя.
     */
    override fun onUpdateReceived(update: Update?) {
        resolveUserInput(update)
            ?.let { filterGroupMessage(it) }
            ?.let { request ->
                runWithMdcContext(rx = processRequest(request))
                    .subscribeOn(Schedulers.boundedElastic())
                    .subscribe()
            }
    }

    /**
     * Отправляет ответ пользователю.
     */
    fun sendResponse(
        chatId: Long,
        requestMessageId: Int? = null,
        responseMessageItem: ResponseMessageItem
    ): Mono<ResponseMessageItem> =
        tryToDeleteMessages(chatId = chatId, responseMessageItem = responseMessageItem)
            .flatMap { tryToEditMessage(chatId = chatId, requestMessageId = requestMessageId, responseMessageItem = it) }
            .flatMap { tryToNotifyErrorGroup(it) }
            .flatMap { tryToSendMessage(chatId = chatId, responseMessageItem = it) }

    private fun resolveUserInput(update: Update?): RequestMessageItem? =
        when {
            update.isTextMessage() -> {
                val inputMessage = update?.message!!
                RequestMessageItem(
                    isTextUpdate = true,
                    isButtonUpdate = false,
                    chatId = inputMessage.chatId,
                    input = inputMessage.text,
                    command = Command.get(inputMessage.text),
                    messageId = inputMessage.messageId
                )
            }

            update.isButtonInput() -> {
                val inputCallback = update?.callbackQuery!!
                RequestMessageItem(
                    isTextUpdate = false,
                    isButtonUpdate = true,
                    chatId = inputCallback.message.chatId,
                    input = inputCallback.data,
                    command = Command.get(inputCallback.data),
                    messageId = inputCallback.message?.messageId!!
                )
            }

            else -> null
        }

    /**
     * Отбрасывает сообщение, если оно было отправлено в группе или супергруппе.
     */
    private fun filterGroupMessage(request: RequestMessageItem): RequestMessageItem? =
        if (request.chatId < 0) null else request

    private fun processRequest(request: RequestMessageItem): Mono<Unit> =
        processorService.process(request)
            .flatMap { sendResponse(chatId = request.chatId, requestMessageId = request.messageId, responseMessageItem = it) }
            .flatMap { postProcessorService.postProcess(request = request, response = it) }

    private fun tryToDeleteMessages(chatId: Long, responseMessageItem: ResponseMessageItem): Mono<ResponseMessageItem> =
        if (responseMessageItem.meta.deleteMessages.active) {
            deleteMessages(telegramId = chatId, deleteMessageIds = responseMessageItem.meta.deleteMessages.messageIds)
                .then(responseMessageItem.toMono())
        } else {
            responseMessageItem.toMono()
        }

    private fun tryToEditMessage(chatId: Long, requestMessageId: Int?, responseMessageItem: ResponseMessageItem): Mono<ResponseMessageItem> =
        if (responseMessageItem.meta.editMessage.active) {
            val messageId = responseMessageItem.meta.editMessage.messageId ?: requestMessageId!!
            editMessage(telegramId = chatId, messageId = messageId, content = responseMessageItem.content!!)
                .then(responseMessageItem.setEditMessageId(messageId).toMono())
        } else {
            responseMessageItem.toMono()
        }

    private fun tryToNotifyErrorGroup(responseMessageItem: ResponseMessageItem): Mono<ResponseMessageItem> =
        if (responseMessageItem.meta.notifyErrorGroup.active) {
            Mono.deferContextual { contextView -> sendTraceIdToErrorGroup(contextView.botTraceId)}
                .map { responseMessageItem }
        } else {
            responseMessageItem.toMono()
        }

    private fun tryToSendMessage(chatId: Long, responseMessageItem: ResponseMessageItem): Mono<ResponseMessageItem> =
        if (responseMessageItem.meta.sendMessage.active) {
            sendMessage(chatId = chatId, content = responseMessageItem.content!!)
                .map { responseMessageItem.setSendMessageId(it.messageId) }
        } else {
            responseMessageItem.toMono()
        }

    // Сделан публичным для того, чтобы мокать в тестах
    fun deleteMessages(telegramId: Long, deleteMessageIds: List<Int>): Mono<Unit> =
        Flux.fromIterable(deleteMessageIds)
            .flatMap { doDeleteMessage(telegramId = telegramId, messageId = it) }
            .then(Mono.empty())

    // Сделан публичным для того, чтобы мокать в тестах
    fun editMessage(telegramId: Long, messageId: Int, content: ResponseMessageItem.Content): Mono<Unit> =
        Mono
            .fromCallable {
                try {
                    execute(
                        TelegramMessageBuilder.editMessage(
                            telegramId = telegramId,
                            messageId = messageId,
                            content = content
                        )
                    )
                } catch (ex: TelegramApiException) {
                    when {
                        ex.isMessageNotModified() -> {}
                        else -> {
                            logger.error("Edit message error", ex)
                            throw ex
                        }
                    }
                }
            }
            .then(Mono.empty())

    // Сделан публичным для того, чтобы мокать в тестах
    fun sendMessage(chatId: Long, content: ResponseMessageItem.Content): Mono<Message> =
        Mono.fromCallable {
            try {
                execute(
                    TelegramMessageBuilder.createMessage(
                        telegramId = chatId,
                        content = content
                    )
                )
            } catch (ex: Throwable) {
                logger.error("Send message error", ex)
                throw ex
            }
        }

    private fun sendTraceIdToErrorGroup(traceId: String?): Mono<Optional<Message>> =
        if (properties.errorGroupNotification.enabled && !traceId.isNullOrEmpty()) {
            sendMessage(
                chatId = properties.errorGroupNotification.chatId!!,
                content = ResponseMessageItem.Content(
                    text = "Код ошибки: $traceId",
                    buttons = emptyList()
                )
            ).map { Optional.of(it) }
        } else {
            Optional.empty<Message>().toMono()
        }

    private fun doDeleteMessage(telegramId: Long, messageId: Int): Mono<Unit> =
        Mono
            .fromCallable {
                try {
                    execute(
                        TelegramMessageBuilder.deleteMessage(
                            telegramId = telegramId,
                            messageId = messageId
                        )
                    )
                } catch (ex: Throwable) {
                    logger.error("Delete message error", ex)
                    throw ex
                }
            }
            .then(Mono.empty())
}