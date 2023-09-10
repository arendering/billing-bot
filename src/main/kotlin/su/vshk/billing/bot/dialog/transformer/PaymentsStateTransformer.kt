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
import su.vshk.billing.bot.message.dto.RequestMessageItem

@Component
class PaymentsStateTransformer(
    private val responseMessageService: ResponseMessageService
): DialogStateTransformer {

    override fun getCommand(): Command =
        Command.PAYMENTS

    override fun initializeState(request: RequestMessageItem, user: UserEntity): Mono<DialogState> =
        Mono.fromCallable {
            DialogState(
                command = getCommand(),
                options = PaymentsOptions(),
                steps = listOf(PaymentsStep.PERIOD),
                response = DialogState.Response.next(responseMessageService.paymentsPeriod())
            )
        }

    override fun processOption(request: RequestMessageItem, user: UserEntity, state: DialogState): Mono<DialogState> =
        Mono.fromCallable {
            when (val step = state.currentStep()) {
                PaymentsStep.PERIOD -> addPeriodToOptions(state = state, option = request.input)
                else -> throw IllegalStateException("unknown step: '$step'")
            }
        }

    private fun addPeriodToOptions(state: DialogState, option: String): DialogState {
        val options = state.options as PaymentsOptions
        return when (option) {
            PaymentsAvailableOptions.PERIOD_ONE_MONTH ->
                state.finish(options.copy(period = PaymentsPeriod.ONE_MONTH))

            PaymentsAvailableOptions.PERIOD_THREE_MONTHS ->
                state.finish(options.copy(period = PaymentsPeriod.THREE_MONTHS))

            PaymentsAvailableOptions.PERIOD_SIX_MONTHS ->
                state.finish(options.copy(period = PaymentsPeriod.SIX_MONTHS))

            else ->
                state.invalidOption(responseMessageService.paymentsWrongPeriod())
        }
    }
}