package su.vshk.billing.bot.dialog.transformer

import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import su.vshk.billing.bot.dao.model.Command
import su.vshk.billing.bot.dao.model.UserEntity
import su.vshk.billing.bot.dialog.dto.DialogState
import su.vshk.billing.bot.dialog.option.NotificationAvailableOptions
import su.vshk.billing.bot.dialog.option.NotificationOptions
import su.vshk.billing.bot.dialog.step.NotificationStep
import su.vshk.billing.bot.message.ResponseMessageService

@Component
class NotificationStateTransformer(
    private val responseMessageService: ResponseMessageService
): DialogStateTransformer {

    override fun getCommand(): Command =
        Command.NOTIFICATION

    override fun initializePreState(user: UserEntity): Mono<DialogState> =
        Mono.fromCallable {
            DialogState(
                command = getCommand(),
                options = NotificationOptions(),
                steps = listOf(NotificationStep.SWITCH),
                messages = createSwitchMessages(user)
            )
        }

    override fun isValidOption(user: UserEntity, state: DialogState, option: String): Boolean =
        (user.paymentNotificationEnabled == true && option == NotificationAvailableOptions.TURN_OFF)
            || (user.paymentNotificationEnabled == false && option == NotificationAvailableOptions.TURN_ON)

    override fun addOption(user: UserEntity, state: DialogState, option: String): DialogState =
        when (val step = state.currentStep()) {
            NotificationStep.SWITCH -> addSwitchOption(state, option)
            else -> throw IllegalStateException("unknown step: '$step'")
        }

    override fun incrementStep(option: String, state: DialogState): DialogState {
        val incrementedStepIndex = state.stepIndex + 1
        return state.copy(stepIndex = incrementedStepIndex)
    }

    private fun createSwitchMessages(user: UserEntity): DialogState.MessageContainer =
        if (user.paymentNotificationEnabled == true) {
            DialogState.MessageContainer(
                message = responseMessageService.notificationDisableMessage(),
                invalidOptionMessage = responseMessageService.notificationInvalidDisableMessage()
            )
        } else {
            DialogState.MessageContainer(
                message = responseMessageService.notificationEnableMessage(),
                invalidOptionMessage = responseMessageService.notificationInvalidEnableMessage()
            )
        }

    private fun addSwitchOption(state: DialogState, option: String): DialogState {
        val options = state.options as NotificationOptions
        when (option) {
            NotificationAvailableOptions.TURN_ON -> options.enable = true
            NotificationAvailableOptions.TURN_OFF -> options.enable = false
            else -> throw IllegalStateException("unreachable code")
        }
        return state.copy(options = options)
    }

}