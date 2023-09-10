package su.vshk.billing.bot.service.processor

import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import reactor.util.context.ContextView
import su.vshk.billing.bot.dao.model.Command
import su.vshk.billing.bot.dao.model.GenericCommand
import su.vshk.billing.bot.dao.model.UserEntity
import su.vshk.billing.bot.dao.service.UserDaoService
import su.vshk.billing.bot.dialog.DialogProcessor
import su.vshk.billing.bot.message.ResponseMessageService
import su.vshk.billing.bot.message.dto.RequestMessageItem
import su.vshk.billing.bot.message.dto.ResponseMessageItem
import su.vshk.billing.bot.service.PaymentNotificationService
import su.vshk.billing.bot.service.executor.CommandExecutor
import su.vshk.billing.bot.util.errorTraceId
import su.vshk.billing.bot.util.getLogger
import su.vshk.billing.bot.util.infoTraceId

@Service
class ProcessorService(
    private val dialogProcessor: DialogProcessor,
    private val responseMessageService: ResponseMessageService,
    private val executors: List<CommandExecutor>,
    private val paymentNotificationService: PaymentNotificationService,
    private val userDaoService: UserDaoService
) {
    companion object {
        private val log = getLogger()
    }

    /**
     * Основная обработка пользовательского запроса.
     */
    fun process(request: RequestMessageItem): Mono<ResponseMessageItem> =
        Mono
            .deferContextual { context ->
                logRequest(context = context, request = request)

                when {
                    request.isButtonUpdate && request.input == GenericCommand.DELETE_PAYMENT_NOTIFICATION ->
                        paymentNotificationService.deletePaymentNotification(request)

                    dialogProcessor.contains(request.chatId) ->
                        updateDialog(request)

                    else ->
                        processCommand(request)
                }
            }
            .onErrorResume {
                Mono.deferContextual { context ->
                    log.errorTraceId(context, it.stackTraceToString())
                    responseMessageService.somethingWentWrongMessage().toMono()
                }
            }

    private fun logRequest(context: ContextView, request: RequestMessageItem) {
        val head = "user '${request.chatId}'"
        val tail = when {
            request.isTextUpdate -> "send text '${request.input}'"
            request.isButtonUpdate -> "push button '${request.input}'"
            else -> throw IllegalStateException("unreachable code")
        }

        log.infoTraceId(context, "$head $tail")
    }

    private fun updateDialog(request: RequestMessageItem): Mono<ResponseMessageItem> =
        if (request.isTextUpdate && dialogProcessor.getCommand(request.chatId) != Command.START) {
            responseMessageService.deleteMessage(request.messageId).toMono()
        } else {
            doUpdateDialog(request)
        }

    private fun processCommand(request: RequestMessageItem): Mono<ResponseMessageItem> =
        userDaoService.findUser(request.chatId)
            .flatMap { userOpt ->
                val command = Command.get(request.input)
                if (userOpt.isPresent) {
                    when {
                        request.isTextUpdate && command == Command.START ->
                            responseMessageService.repeatMenuMessage(request.messageId).toMono()

                        request.isTextUpdate ->
                            responseMessageService.deleteMessage(request.messageId).toMono()

                        request.isButtonUpdate ->
                            doProcessCommand(request = request, user = userOpt.get(), command = command ?: Command.MENU)

                        else -> throw RuntimeException("unreachable code")
                    }
                } else {
                    doProcessCommand(
                        request = request,
                        user = UserEntity(telegramId = request.chatId),
                        command = Command.START
                    )
                }
            }

    private fun doUpdateDialog(request: RequestMessageItem): Mono<ResponseMessageItem> =
        dialogProcessor.processOption(request)
            .flatMap { dto ->
                val state = dto.state
                if (state.isFinished) {
                    findExecutor(state.command!!).execute(user = dto.user, options = state.options)
                } else {
                    state.responseMessageItem!!.toMono()
                }
            }

    private fun doProcessCommand(request: RequestMessageItem, user: UserEntity, command: Command): Mono<ResponseMessageItem> =
        if (command.isDialog) {
            dialogProcessor
                .startDialog(request = request, user = user, command = command)
                .map { it.state.responseMessageItem!! }
        } else {
            findExecutor(command).execute(user = user)
        }

    private fun findExecutor(command: Command): CommandExecutor =
        executors.find { it.getCommand() == command }
            ?: throw RuntimeException("could not find executor for command '${command.value}'")
}