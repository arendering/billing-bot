package su.vshk.billing.bot.dialog.transformer

import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import su.vshk.billing.bot.dao.model.Command
import su.vshk.billing.bot.dao.model.UserEntity
import su.vshk.billing.bot.dialog.dto.DialogState
import su.vshk.billing.bot.dialog.option.ExitAvailableOptions
import su.vshk.billing.bot.dialog.step.ExitStep
import su.vshk.billing.bot.message.ResponseMessageService

@Component
class ExitStateTransformer(
    private val responseMessageService: ResponseMessageService
): DialogStateTransformer {

    override fun getCommand(): Command =
        Command.EXIT

    override fun initializePreState(user: UserEntity): Mono<DialogState> =
        Mono.fromCallable {
            DialogState(
                command = getCommand(),
                steps = listOf(ExitStep.WARNING),
                messages = DialogState.MessageContainer(
                    message = responseMessageService.logoutWarningMessage(),
                    invalidOptionMessage = responseMessageService.logoutInvalidWarningMessage()
                )
            )
        }

    override fun isValidOption(user: UserEntity, state: DialogState, option: String): Boolean =
        option == ExitAvailableOptions.YES

    override fun addOption(user: UserEntity, state: DialogState, option: String): DialogState =
        when (val step = state.currentStep()) {
            ExitStep.WARNING -> state
            else -> throw IllegalStateException("unknown step: '$step'")
        }

    override fun incrementStep(option: String, state: DialogState): DialogState {
        val incrementedStepIndex = state.stepIndex + 1
        return state.copy(stepIndex = incrementedStepIndex)
    }

}