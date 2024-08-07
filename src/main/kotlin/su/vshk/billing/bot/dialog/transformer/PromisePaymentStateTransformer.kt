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
import su.vshk.billing.bot.service.CalculatorService
import su.vshk.billing.bot.service.RecommendedPaymentService
import su.vshk.billing.bot.util.AmountUtils

@Component
class PromisePaymentStateTransformer(
    private val recommendedPaymentService: RecommendedPaymentService,
    private val promisePaymentMessageService: PromisePaymentMessageService,
    private val calculatorService: CalculatorService
): DialogStateTransformer {

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
                    if (recommendedAmount > calculatorService.getMaxLimit(getCommand())) {
                        state.cancel(promisePaymentMessageService.showDebtsOverdueError())
                    } else {
                        val response = calculatorService.initialize(telegramId = user.telegramId, amount = recommendedAmount, command = getCommand())
                        state.incrementStep(options = state.options, responseMessageItem = response)
                    }
                }
        } else {
            throw RuntimeException("step '${state.currentStep()}': unexpected option '$option'")
        }

    private fun processAmountOption(user: UserEntity, state: DialogState, option: String): DialogState {
        val dto = calculatorService.processOption(telegramId = user.telegramId, option = option)
        return when {
            dto.next -> state.stayCurrentStep(dto.response!!)
            dto.cancel -> state.cancel(dto.response!!)
            dto.finish -> {
                val updatedOptions = (state.options as PromisePaymentOptions).copy(amount = dto.amount)
                state.finish(updatedOptions)
            }
            else -> throw RuntimeException("inconsistent calculator dto $dto")
        }
    }

    private fun getActualRecommendedPayment(user: UserEntity): Mono<Int> =
        recommendedPaymentService.getActual(user.agreementId!!)
            .map { AmountUtils.integerRound(it) }
}