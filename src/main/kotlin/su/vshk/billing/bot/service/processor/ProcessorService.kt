package su.vshk.billing.bot.service.processor

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import su.vshk.billing.bot.dao.model.Command
import su.vshk.billing.bot.dao.model.UserEntity
import su.vshk.billing.bot.dao.service.UserDaoService
import su.vshk.billing.bot.dialog.DialogProcessor
import su.vshk.billing.bot.message.dto.RequestMessageItem
import su.vshk.billing.bot.message.dto.ResponseMessageItem
import su.vshk.billing.bot.message.response.CommonMessageService
import su.vshk.billing.bot.service.executor.CommandExecutor
import su.vshk.billing.bot.util.InternalException
import su.vshk.billing.bot.util.getLogger

@Service
class ProcessorService(
    private val dialogProcessor: DialogProcessor,
    private val executors: List<CommandExecutor>,
    private val userDaoService: UserDaoService,
    private val commonMessageService: CommonMessageService,
    private val objectMapper: ObjectMapper
) {

    private val logger = getLogger()

    /**
     * Основная обработка пользовательского запроса.
     */
    fun process(request: RequestMessageItem): Mono<ResponseMessageItem> =
        Mono
            .defer {
                logger.info("--> Bot request ${request.toJson()}")

                when {
                    request.command?.isService == true ->
                        doProcessCommand(request = request, user = null, actualCommand = request.command)

                    dialogProcessor.contains(request.chatId) ->
                        updateDialog(request)

                    else ->
                        processCommand(request)
                }
            }
            .map { response ->
                logger.info("<-- Bot response ${response.toJson()}")
                response
            }
            .onErrorResume { th ->
                logger.error(th.stackTraceToString())
                commonMessageService.showGenericError().toMono()
            }

    private fun updateDialog(request: RequestMessageItem): Mono<ResponseMessageItem> =
        when {
            request.isButtonUpdate || isLoginOption(request) ->
                doUpdateDialog(request)

            isStartInput(request) ->
                commonMessageService.repeatLastDialogMessage(
                    messageIdToDelete = request.messageId,
                    content = dialogProcessor.getLastResponseMessageItemContent(request.chatId)!!
                ).toMono()

            request.isTextUpdate ->
                commonMessageService.deleteMessage(request.messageId).toMono()

            else -> throw InternalException("unable to update dialog")
        }

    private fun processCommand(request: RequestMessageItem): Mono<ResponseMessageItem> =
        userDaoService.findUser(request.chatId)
            .flatMap { userOpt ->
                when {
                    userOpt.isEmpty ->
                        doProcessCommand(request = request, user = UserEntity(telegramId = request.chatId), actualCommand = Command.LOGIN)

                    isStartInput(request) ->
                        commonMessageService.repeatMenu(request.messageId).toMono()

                    request.isTextUpdate ->
                        commonMessageService.deleteMessage(request.messageId).toMono()

                    request.isButtonUpdate ->
                        doProcessCommand(request = request, user = userOpt.get(), actualCommand = request.command ?: Command.MENU)

                    else -> throw InternalException("unable to process command")
                }
            }

    private fun doUpdateDialog(request: RequestMessageItem): Mono<ResponseMessageItem> =
        dialogProcessor.processOption(request)
            .flatMap { dto ->
                val state = dto.state
                if (state.isFinished) {
                    findExecutor(state.command!!).execute(request = request, user = dto.user, options = state.options)
                } else {
                    state.responseMessageItem!!.toMono()
                }
            }

    private fun doProcessCommand(request: RequestMessageItem, user: UserEntity?, actualCommand: Command): Mono<ResponseMessageItem> =
        if (actualCommand.isDialog) {
            dialogProcessor
                .startDialog(request = request, user = user!!, command = actualCommand)
                .map { it.state.responseMessageItem!! }
        } else {
            findExecutor(actualCommand).execute(request = request, user = user, options = null)
        }

    private fun findExecutor(command: Command): CommandExecutor =
        executors.find { it.getCommand() == command }
            ?: throw InternalException("unable to find executor for command '${command.value}'")

    private fun isLoginOption(request: RequestMessageItem): Boolean =
        request.isTextUpdate && dialogProcessor.getCommand(request.chatId) == Command.LOGIN

    private fun isStartInput(request: RequestMessageItem): Boolean =
        request.isTextUpdate && request.command?.isStart() == true

    private fun <T> T.toJson(): String =
        objectMapper.writeValueAsString(this)
}