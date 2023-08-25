package su.vshk.billing.bot.dialog.transformer

import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import su.vshk.billing.bot.dao.model.Command
import su.vshk.billing.bot.dao.model.UserEntity
import su.vshk.billing.bot.dialog.dto.DialogState
import su.vshk.billing.bot.dialog.option.LoginOptions
import su.vshk.billing.bot.dialog.step.LoginStep
import su.vshk.billing.bot.message.ResponseMessageService

@Component
class LoginStateTransformer(
    private val responseMessageService: ResponseMessageService
): DialogStateTransformer {

    override fun getCommand(): Command =
        Command.START

    override fun initializePreState(user: UserEntity): Mono<DialogState> =
        Mono.fromCallable {
            DialogState(
                command = getCommand(),
                options = LoginOptions(),
                steps = listOf(LoginStep.LOGIN, LoginStep.PASSWORD),
                messages = DialogState.MessageContainer(
                    message = responseMessageService.startLoginMessage()
                )
            )
        }

    override fun addOption(user: UserEntity, state: DialogState, option: String): DialogState {
        val options = state.options as LoginOptions
        when (val step = state.currentStep()) {
            LoginStep.LOGIN -> options.login = option
            LoginStep.PASSWORD -> options.password = option
            else -> throw IllegalStateException("unknown step: '$step'")
        }
        return state.copy(options = options)
    }

    override fun incrementStep(option: String, state: DialogState): DialogState {
        val incrementedStepIndex = state.stepIndex + 1
        return state.copy(
            stepIndex = incrementedStepIndex,
            messages = DialogState.MessageContainer(
                message = responseMessageService.startPasswordMessage()
            )
        )
    }

}