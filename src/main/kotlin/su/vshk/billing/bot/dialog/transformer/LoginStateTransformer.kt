package su.vshk.billing.bot.dialog.transformer

import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import su.vshk.billing.bot.dao.model.Command
import su.vshk.billing.bot.dao.model.UserEntity
import su.vshk.billing.bot.dialog.dto.DialogState
import su.vshk.billing.bot.dialog.option.LoginOptions
import su.vshk.billing.bot.dialog.step.LoginStep
import su.vshk.billing.bot.message.ResponseMessageService
import su.vshk.billing.bot.message.dto.RequestMessageItem
import su.vshk.billing.bot.service.LoginMessageService

@Component
class LoginStateTransformer(
    private val responseMessageService: ResponseMessageService,
    private val loginMessageService: LoginMessageService
): DialogStateTransformer {

    override fun getCommand(): Command =
        Command.START

    override fun initializeState(request: RequestMessageItem, user: UserEntity): Mono<DialogState> =
        loginMessageService.init(request)
            .map { _ ->
                DialogState(
                    command = getCommand(),
                    options = LoginOptions(),
                    steps = listOf(LoginStep.LOGIN, LoginStep.PASSWORD),
                    response = DialogState.Response.next(responseMessageService.startLoginMessage())
                )
            }

    override fun processOption(request: RequestMessageItem, user: UserEntity, state: DialogState): Mono<DialogState> =
        Mono.defer {
            val options = state.options as LoginOptions
            val option = request.input

            when (val step = state.currentStep()) {
                LoginStep.LOGIN ->
                    loginMessageService.add(telegramId = request.chatId, messageId = request.messageId)
                        .map { _ ->
                            state.incrementStep(
                                options = options.copy(login = option),
                                responseMessageItem = responseMessageService.startPasswordMessage()
                            )
                        }

                LoginStep.PASSWORD ->
                    loginMessageService.add(telegramId = request.chatId, messageId = request.messageId)
                        .map { state.finish(options.copy(password = option)) }

                else -> throw IllegalStateException("unknown step: '$step'")
            }
        }
}