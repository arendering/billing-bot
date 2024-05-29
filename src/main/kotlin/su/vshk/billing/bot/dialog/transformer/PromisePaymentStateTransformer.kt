package su.vshk.billing.bot.dialog.transformer

import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import su.vshk.billing.bot.dao.model.Command
import su.vshk.billing.bot.dao.model.UserEntity
import su.vshk.billing.bot.dialog.dto.DialogState
import su.vshk.billing.bot.dialog.option.PromisePaymentAvailableOptions
import su.vshk.billing.bot.dialog.option.PromisePaymentOptions
import su.vshk.billing.bot.dialog.step.PromisePaymentStep
import su.vshk.billing.bot.message.dto.RequestMessageItem
import su.vshk.billing.bot.message.response.PromisePaymentMessageService
import su.vshk.billing.bot.service.RecommendedPaymentService
import su.vshk.billing.bot.util.AmountUtils
import java.util.concurrent.ConcurrentHashMap

@Component
class PromisePaymentStateTransformer(
    private val recommendedPaymentService: RecommendedPaymentService,
    private val promisePaymentMessageService: PromisePaymentMessageService
): DialogStateTransformer {

    companion object {
        private const val AMOUNT_LOWER_BOUND = 1

        private const val AMOUNT_UPPER_BOUND = 1500

        /**
         * Хранит текущее значение обещанного платежа для пользователя на шаге ввода суммы платежа.
         * Key - telegramId, value - значение обещанного платежа
         */
        private val promisePaymentCache = ConcurrentHashMap<Long, Int>()
    }

    override fun getCommand(): Command =
        Command.PROMISE_PAYMENT

    override fun initializeState(request: RequestMessageItem, user: UserEntity): Mono<DialogState> =
        Mono.fromCallable {
            DialogState(
                command = getCommand(),
                options = PromisePaymentOptions(),
                steps = listOf(PromisePaymentStep.WARNING, PromisePaymentStep.AMOUNT),
                response = DialogState.Response.next(promisePaymentMessageService.showWarning())
            )
        }

    override fun processOption(request: RequestMessageItem, user: UserEntity, state: DialogState): Mono<DialogState> =
        Mono.defer {
            when (val step = state.currentStep()) {
                PromisePaymentStep.WARNING ->
                    processWarningOption(user = user, state = state, option = request.input)

                PromisePaymentStep.AMOUNT ->
                    processAmountOption(user = user, state = state, option = request.input).toMono()

                else -> IllegalStateException("unknown step: '$step'").toMono()
            }
        }

    private fun processWarningOption(user: UserEntity, state: DialogState, option: String): Mono<DialogState> =
        if (option == PromisePaymentAvailableOptions.WARNING_APPROVE) {
            getActualRecommendedPayment(user)
                .map { recommendedAmount ->
                    if (recommendedAmount > AMOUNT_UPPER_BOUND) {
                        state.cancel(promisePaymentMessageService.showDebtsOverdueError())
                    } else {
                        promisePaymentCache[user.telegramId] = recommendedAmount
                        state.incrementStep(
                            options = state.options,
                            responseMessageItem = promisePaymentMessageService.showCalculator(recommendedAmount)
                        )
                    }
                }
        } else {
            throw RuntimeException("step '${state.currentStep()}': unexpected option '$option'")
        }

    private fun processAmountOption(user: UserEntity, state: DialogState, option: String): DialogState =
        when (option) {
            PromisePaymentAvailableOptions.CANCEL_AMOUNT_STEP -> {
                promisePaymentCache.remove(user.telegramId)
                state.cancel(promisePaymentMessageService.showMainMenu())
            }

            PromisePaymentAvailableOptions.AMOUNT_SUBMIT -> {
                val promisePaymentAmount = promisePaymentCache.remove(user.telegramId)
                    ?: throw RuntimeException("promise payment amount not found in cache by key '${user.telegramId}'")
                val updatedOptions = (state.options as PromisePaymentOptions).copy(amount = promisePaymentAmount)
                state.finish(updatedOptions)
            }

            in PromisePaymentAvailableOptions.calculatorOptions ->
                doProcessAmountOption(user = user, state = state, option = option)

            else -> {
                promisePaymentCache.remove(user.telegramId)
                throw RuntimeException("step '${state.currentStep()}': unexpected option '$option'")
            }
        }

    private fun doProcessAmountOption(user: UserEntity, state: DialogState, option: String): DialogState {
        val amount = promisePaymentCache[user.telegramId]
            ?: throw RuntimeException("promise payment amount not found in cache by key '${user.telegramId}'")

        val updatedAmount = doUpdateAmount(currentAmount = amount, option = option)

        return when {
            updatedAmount < AMOUNT_LOWER_BOUND ->
                state.stayCurrentStep(
                    promisePaymentMessageService.showCalculatorWithTooLowAmount(amount = amount , lowerBound = AMOUNT_LOWER_BOUND)
                )

            updatedAmount > AMOUNT_UPPER_BOUND ->
                state.stayCurrentStep(
                    promisePaymentMessageService.showCalculatorWithTooHighAmount(amount = amount, upperBound = AMOUNT_UPPER_BOUND)
                )

            else -> {
                promisePaymentCache[user.telegramId] = updatedAmount
                state.stayCurrentStep(
                    promisePaymentMessageService.showCalculator(updatedAmount)
                )
            }
        }
    }

    private fun getActualRecommendedPayment(user: UserEntity): Mono<Int> =
        recommendedPaymentService.getActual(user.agreementId!!)
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

}