package su.vshk.billing.bot.dialog.transformer

import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import su.vshk.billing.bot.dao.model.Command
import su.vshk.billing.bot.dao.model.UserEntity
import su.vshk.billing.bot.dialog.dto.DialogState
import su.vshk.billing.bot.dialog.option.PaymentHistoryAvailableOptions
import su.vshk.billing.bot.dialog.option.PaymentHistoryOptions
import su.vshk.billing.bot.dialog.option.PaymentHistoryPeriod
import su.vshk.billing.bot.dialog.step.PaymentHistoryStep
import su.vshk.billing.bot.message.dto.RequestMessageItem
import su.vshk.billing.bot.message.response.PaymentHistoryMessageService

@Component
class PaymentHistoryStateTransformer(
    private val paymentHistoryMessageService: PaymentHistoryMessageService
): DialogStateTransformer {

    override fun getCommand(): Command =
        Command.PAYMENT_HISTORY

    override fun initializeState(request: RequestMessageItem, user: UserEntity): Mono<DialogState> =
        Mono.fromCallable {
            DialogState(
                command = getCommand(),
                options = PaymentHistoryOptions(),
                steps = listOf(PaymentHistoryStep.PERIOD),
                response = DialogState.Response.next(paymentHistoryMessageService.showHistoryPeriods())
            )
        }

    override fun processOption(request: RequestMessageItem, user: UserEntity, state: DialogState): Mono<DialogState> =
        Mono.fromCallable {
            when (val step = state.currentStep()) {
                PaymentHistoryStep.PERIOD -> addPeriodToOptions(state = state, option = request.input)
                else -> throw IllegalStateException("unknown step: '$step'")
            }
        }

    private fun addPeriodToOptions(state: DialogState, option: String): DialogState {
        val options = state.options as PaymentHistoryOptions
        return when (option) {
            PaymentHistoryAvailableOptions.PERIOD_ONE_MONTH ->
                state.finish(options.copy(period = PaymentHistoryPeriod.ONE_MONTH))

            PaymentHistoryAvailableOptions.PERIOD_THREE_MONTHS ->
                state.finish(options.copy(period = PaymentHistoryPeriod.THREE_MONTHS))

            PaymentHistoryAvailableOptions.PERIOD_SIX_MONTHS ->
                state.finish(options.copy(period = PaymentHistoryPeriod.SIX_MONTHS))

            else ->
                throw RuntimeException("step ${state.currentStep()}: unexpected option '$option'")
        }
    }
}