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
    private val responseMessageContentFormatter: ResponseMessageContentFormatter,
    private val processorService: ProcessorService,
    private val postProcessorService: PostProcessorService
): TelegramLongPollingBot() {

    companion object {
        private val log = getLogger()
    }

    override fun getBotToken(): String =
        properties.token!!

    override fun getBotUsername(): String =
        properties.name!!

    /**
     * Точка входа для обработки сообщения от пользователя.
     */
    override fun onUpdateReceived(update: Update?) {
        resolveUserInput(update)
            ?.let { request ->
                processorService.process(request)
                    .flatMap { sendResponse(chatId = request.chatId, requestMessageId = request.messageId, responseMessageItem = it) }
                    .flatMap { postProcessorService.postProcess(request = request, response = it) }
                    .putTraceId()
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
                    messageId = inputCallback.message?.messageId!!
                )
            }

            else -> null
        }

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
            Mono.deferContextual { contextView -> sendTraceIdToErrorGroup(contextView.traceId)}
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
        Mono.deferContextual { contextView ->
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
                                log.errorTraceId(context = contextView, msg = "edit message error", ex = ex)
                                throw ex
                            }
                        }
                    }
                }
                .then(Mono.empty())
        }

    // Сделан публичным для того, чтобы мокать в тестах
    fun sendMessage(chatId: Long, content: ResponseMessageItem.Content): Mono<Message> =
        Mono.deferContextual { contextView ->
            Mono.fromCallable {
                try {
                    execute(
                        TelegramMessageBuilder.createMessage(
                            telegramId = chatId,
                            content = content
                        )
                    )
                } catch (ex: Throwable) {
                    log.errorTraceId(context = contextView, msg = "send message error", ex = ex)
                    throw ex
                }
            }
        }

    //TODO: это должен делать zabbix
    private fun sendTraceIdToErrorGroup(traceId: String): Mono<Optional<Message>> =
        if (properties.errorGroupNotification.enabled) {
            sendMessage(
                chatId = properties.errorGroupNotification.chatId!!,
                content = responseMessageContentFormatter.errorGroupSomethingWentWrong(traceId)
            ).map { Optional.of(it) }
        } else {
            Optional.empty<Message>().toMono()
        }

    private fun doDeleteMessage(telegramId: Long, messageId: Int): Mono<Unit> =
        Mono.deferContextual { contextView ->
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
                        log.errorTraceId(context = contextView, msg = "delete message error", ex = ex)
                        throw ex
                    }
                }
                .then(Mono.empty())
        }
}