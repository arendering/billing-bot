package su.vshk.billing.bot.dialog.transformer

import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import su.vshk.billing.bot.dao.model.Command
import su.vshk.billing.bot.dao.model.UserEntity
import su.vshk.billing.bot.dialog.dto.DialogState
import su.vshk.billing.bot.dialog.option.PromisePaymentAvailableOptions
import su.vshk.billing.bot.dialog.option.PromisePaymentOptions
import su.vshk.billing.bot.dialog.step.PromisePaymentAmountStepData
import su.vshk.billing.bot.dialog.step.PromisePaymentStep
import su.vshk.billing.bot.message.ResponseMessageService
import su.vshk.billing.bot.message.dto.RequestMessageItem
import su.vshk.billing.bot.service.RecommendedPaymentService
import su.vshk.billing.bot.util.AmountUtils

@Component
class PromisePaymentStateTransformer(
    private val recommendedPaymentService: RecommendedPaymentService,
    private val responseMessageService: ResponseMessageService
): DialogStateTransformer {

    companion object {
        private const val AMOUNT_LOWER_BOUND = 1
        private const val AMOUNT_UPPER_BOUND = 1500
    }

    override fun getCommand(): Command =
        Command.PROMISE_PAYMENT

    override fun initializeState(request: RequestMessageItem, user: UserEntity): Mono<DialogState> =
        Mono.fromCallable {
            DialogState(
                command = getCommand(),
                options = PromisePaymentOptions(),
                steps = listOf(PromisePaymentStep.WARNING, PromisePaymentStep.AMOUNT),
                response = DialogState.Response.next(responseMessageService.clientPromisePaymentWarningMessage())
            )
        }

    override fun processOption(request: RequestMessageItem, user: UserEntity, state: DialogState): Mono<DialogState> =
        Mono.defer {
            when (val step = state.currentStep()) {
                PromisePaymentStep.WARNING ->
                    processWarningOption(user = user, state = state, option = request.input)

                PromisePaymentStep.AMOUNT ->
                    processAmountOption(state = state, option = request.input).toMono()

                else -> IllegalStateException("unknown step: '$step'").toMono()
            }
        }

    private fun processWarningOption(user: UserEntity, state: DialogState, option: String): Mono<DialogState> =
        if (option == PromisePaymentAvailableOptions.WARNING_APPROVE) {
            getActualRecommendedPayment(user)
                .map { recommendedAmount ->
                    if (recommendedAmount > AMOUNT_UPPER_BOUND) {
                        state.cancel(responseMessageService.clientPromisePaymentDebtsOverdueMessage())
                    } else {
                        state.incrementStep(
                            options = state.options,
                            stepData = PromisePaymentAmountStepData(
                                amount = recommendedAmount
                            ),
                            responseMessageItem = responseMessageService.clientPromisePaymentAmountMessage(recommendedAmount)
                        )
                    }
                }
        } else {
            state.invalidOption(responseMessageService.clientPromisePaymentInvalidWarningMessage()).toMono()
        }

    private fun processAmountOption(state: DialogState, option: String): DialogState =
        when (option) {
            PromisePaymentAvailableOptions.AMOUNT_SUBMIT -> {
                val updatedOptions = (state.options as PromisePaymentOptions).copy(amount = state.amount())
                state.finish(updatedOptions)
            }

            in PromisePaymentAvailableOptions.calculatorOptions ->
                doProcessAmountOption(state = state, option = option)

            else ->
                state.invalidOption(responseMessageService.clientPromisePaymentAmountMessage(state.amount()))
        }

    private fun doProcessAmountOption(state: DialogState, option: String): DialogState {
        val updatedAmount = doUpdateAmount(state.amount(), option)
        return when {
            updatedAmount < AMOUNT_LOWER_BOUND ->
                state.invalidOption(
                    responseMessageService.clientPromisePaymentTooLowAmountMessage(amount = state.amount(), lowerBound = AMOUNT_LOWER_BOUND)
                )

            updatedAmount > AMOUNT_UPPER_BOUND ->
                state.invalidOption(
                    responseMessageService.clientPromisePaymentTooHighAmountMessage(amount = state.amount(), upperBound = AMOUNT_UPPER_BOUND)
                )

            else -> {
                val stepData = state.stepData as PromisePaymentAmountStepData
                state.updateStepData(
                    stepData = stepData.copy(amount = updatedAmount),
                    responseMessageItem = responseMessageService.clientPromisePaymentAmountMessage(updatedAmount)
                )
            }
        }
    }

    private fun getActualRecommendedPayment(user: UserEntity): Mono<Int> =
        recommendedPaymentService.getActual(user.agrmId!!)
            .map { AmountUtils.integerRound(it) }

    private fun doUpdateAmount(currentAmount: Int, option: String): Int =
        when (option) {
            PromisePaymentAvailableOptions.AMOUNT_PLUS_ONE -> currentAmount + 1
            PromisePaymentAvailableOptions.AMOUNT_MINUS_ONE -> currentAmount - 1
            PromisePaymentAvailableOptions.AMOUNT_PLUS_TWENTY_FIVE -> currentAmount + 25
            PromisePaymentAvailableOptions.AMOUNT_MINUS_TWENTY_FIVE -> currentAmount - 25
            PromisePaymentAvailableOptions.AMOUNT_PLUS_ONE_HUNDRED -> currentAmount + 100
            PromisePaymentAvailableOptions.AMOUNT_MINUS_ONE_HUNDRED -> currentAmount - 100
            else -> currentAmount
        }

    private fun DialogState.amount(): Int =
        (this.stepData as PromisePaymentAmountStepData).amount!!
}