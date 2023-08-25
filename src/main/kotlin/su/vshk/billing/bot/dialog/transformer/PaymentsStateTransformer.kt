package su.vshk.billing.bot.dialog.transformer

import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import su.vshk.billing.bot.dao.model.Command
import su.vshk.billing.bot.dao.model.UserEntity
import su.vshk.billing.bot.dialog.dto.DialogState
import su.vshk.billing.bot.dialog.option.PaymentsAvailableOptions
import su.vshk.billing.bot.dialog.option.PaymentsOptions
import su.vshk.billing.bot.dialog.option.PaymentsPeriod
import su.vshk.billing.bot.dialog.step.PaymentsStep
import su.vshk.billing.bot.message.ResponseMessageService

@Component
class PaymentsStateTransformer(
    private val responseMessageService: ResponseMessageService
): DialogStateTransformer {

    override fun getCommand(): Command =
        Command.PAYMENTS

    override fun initializePreState(user: UserEntity): Mono<DialogState> =
        Mono.fromCallable {
            DialogState(
                command = getCommand(),
                options = PaymentsOptions(),
                steps = listOf(PaymentsStep.PERIOD),
                messages = DialogState.MessageContainer(
                    message = responseMessageService.paymentsPeriod(),
                    invalidOptionMessage = responseMessageService.paymentsWrongPeriod()
                )
            )
        }

    override fun isValidOption(user: UserEntity, state: DialogState, option: String): Boolean =
        option in PaymentsAvailableOptions.availablePeriods

    override fun addOption(user: UserEntity, state: DialogState, option: String): DialogState =
        when (val step = state.currentStep()) {
            PaymentsStep.PERIOD -> addPeriodToOptions(state, option)
            else -> throw IllegalStateException("unknown step: '$step'")
        }

    override fun incrementStep(option: String, state: DialogState): DialogState {
        val incrementedStepIndex = state.stepIndex + 1
        return state.copy(stepIndex = incrementedStepIndex)
    }

    private fun addPeriodToOptions(state: DialogState, option: String): DialogState {
        val options = state.options as PaymentsOptions
        when (option) {
            PaymentsAvailableOptions.PERIOD_ONE_MONTH -> options.period = PaymentsPeriod.ONE_MONTH
            PaymentsAvailableOptions.PERIOD_THREE_MONTHS -> options.period = PaymentsPeriod.THREE_MONTHS
            PaymentsAvailableOptions.PERIOD_SIX_MONTHS -> options.period = PaymentsPeriod.SIX_MONTHS
            else -> throw IllegalStateException("unreachable code")
        }
        return state.copy(options = options)
    }
}