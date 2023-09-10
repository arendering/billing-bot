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
import su.vshk.billing.bot.message.dto.RequestMessageItem

@Component
class NotificationStateTransformer(
    private val responseMessageService: ResponseMessageService
): DialogStateTransformer {

    override fun getCommand(): Command =
        Command.NOTIFICATION

    override fun initializeState(request: RequestMessageItem, user: UserEntity): Mono<DialogState> =
        Mono.fromCallable {
            DialogState(
                command = getCommand(),
                options = NotificationOptions(),
                steps = listOf(NotificationStep.SWITCH),
                response = DialogState.Response.next(
                    if (user.paymentNotificationEnabled == true) {
                        responseMessageService.notificationDisableMessage()
                    } else {
                        responseMessageService.notificationEnableMessage()
                    }
                )
            )
        }

    override fun processOption(request: RequestMessageItem, user: UserEntity, state: DialogState): Mono<DialogState> =
        Mono.fromCallable {
            when (val step = state.currentStep()) {
                NotificationStep.SWITCH -> addSwitchOption(user = user, state = state, option = request.input)
                else -> throw IllegalStateException("unknown step: '$step'")
            }
        }

    private fun addSwitchOption(user: UserEntity, state: DialogState, option: String): DialogState {
        val options = state.options as NotificationOptions
        return when {
            user.paymentNotificationEnabled == true && option == NotificationAvailableOptions.TURN_OFF ->
                state.finish(
                    options.copy(enable = false)
                )

            user.paymentNotificationEnabled == true && option != NotificationAvailableOptions.TURN_OFF ->
                state.invalidOption(responseMessageService.notificationInvalidDisableMessage())

            user.paymentNotificationEnabled == false && option == NotificationAvailableOptions.TURN_ON ->
                state.finish(
                    options.copy(enable = true)
                )

            user.paymentNotificationEnabled == false && option != NotificationAvailableOptions.TURN_ON ->
                state.invalidOption(responseMessageService.notificationInvalidEnableMessage())

            else -> throw IllegalStateException("unreachable code")
        }
    }
}