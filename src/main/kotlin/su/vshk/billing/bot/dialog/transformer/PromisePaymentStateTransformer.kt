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
import su.vshk.billing.bot.message.ResponseMessageContentFormatter
import su.vshk.billing.bot.message.ResponseMessageService
import su.vshk.billing.bot.service.RecommendedPaymentService
import su.vshk.billing.bot.util.AmountUtils

@Component
class PromisePaymentStateTransformer(
    private val recommendedPaymentService: RecommendedPaymentService,
    private val responseMessageContentFormatter: ResponseMessageContentFormatter,
    private val responseMessageService: ResponseMessageService
): DialogStateTransformer {

    companion object {
        private const val AMOUNT_LOWER_BOUND = 1
        private const val AMOUNT_UPPER_BOUND = 1500
    }

    override fun getCommand(): Command =
        Command.PROMISE_PAYMENT

    override fun initializePreState(user: UserEntity): Mono<DialogState> =
        Mono.fromCallable {
            DialogState(
                command = getCommand(),
                options = PromisePaymentOptions(),
                steps = listOf(PromisePaymentStep.WARNING, PromisePaymentStep.AMOUNT),
                messages = createWarningStepMessages()
            )
        }

    override fun initializeStepData(user: UserEntity, state: DialogState): Mono<DialogState> =
        Mono.defer {
            when (val step = state.currentStep()) {
                PromisePaymentStep.WARNING -> state.toMono()

                PromisePaymentStep.AMOUNT ->
                    state.stepData
                        ?.let { state.toMono() }
                        ?: getActualRecommendedPayment(user)
                            .map { state.copy(stepData = PromisePaymentAmountStepData(recommendedAmount = it, amount = it)) }

                else -> IllegalStateException("unknown step: '$step'").toMono()
            }
        }

    override fun isValidStep(user: UserEntity, state: DialogState): Boolean =
        when (val step = state.currentStep()) {
            PromisePaymentStep.WARNING -> true
            //TODO: придумать механизм, который позволял бы не валидировать stepData, если не было перехода на следующий шаг диалога
            PromisePaymentStep.AMOUNT -> state.recommendedAmount() <= AMOUNT_UPPER_BOUND
            else -> throw IllegalStateException("unknown step: '$step'")
        }

    override fun isValidOption(user: UserEntity, state: DialogState, option: String): Boolean =
        when (val step = state.currentStep()) {
            PromisePaymentStep.WARNING ->
                option == PromisePaymentAvailableOptions.WARNING_APPROVE

            PromisePaymentStep.AMOUNT ->
                option in PromisePaymentAvailableOptions.allAmountOptions
                    && doUpdateAmount(state.amount(), option) in AMOUNT_LOWER_BOUND ..AMOUNT_UPPER_BOUND

            else -> throw IllegalStateException("unknown step: '$step'")
        }

    override fun addOption(user: UserEntity, state: DialogState, option: String): DialogState =
        when (val step = state.currentStep()) {
            PromisePaymentStep.WARNING -> state

            PromisePaymentStep.AMOUNT ->
                if (option == PromisePaymentAvailableOptions.AMOUNT_SUBMIT) {
                    addAmountToOptions(state)
                } else {
                    state.copy(stepData = updateAmountStepData(state, option))
                }

            else -> throw IllegalStateException("unknown step: '$step'")
        }

    override fun incrementStep(option: String, state: DialogState): DialogState =
        when (val step = state.currentStep()) {
            PromisePaymentStep.WARNING -> {
                val updatedStepIndex = state.stepIndex + 1
                state.copy(
                    stepIndex = updatedStepIndex,
                    messages = DialogState.MessageContainer(
                        message = responseMessageService.clientPromisePaymentAmountMessage(),
                        templateText = responseMessageContentFormatter.clientPromisePaymentAmountTextTemplate(),
                        invalidStepMessage = responseMessageService.clientPromisePaymentDebtsOverdueMessage(),
                        invalidOptionMessage = responseMessageService.clientPromisePaymentAmountMessage(),
                        invalidOptionTemplateText = responseMessageContentFormatter.clientPromisePaymentAmountInvalidOptionTextTemplate()
                    )
                )
            }

            PromisePaymentStep.AMOUNT ->
                if (option == PromisePaymentAvailableOptions.AMOUNT_SUBMIT) {
                    val inc = state.stepIndex + 1
                    state.copy(stepIndex = inc)
                } else {
                    state
                }

            else -> throw IllegalStateException("unknown step: '$step'")
        }

    override fun transformMessage(state: DialogState): DialogState =
        when (val step = state.currentStep()) {
            PromisePaymentStep.WARNING -> state

            PromisePaymentStep.AMOUNT -> {
                val messages = state.messages!!

                val updatedText = messages.templateText?.format(state.amount())!!
                val message = messages.message!!

                val updatedContent = message.content?.copy(text = updatedText)
                val updatedMessage = message.copy(content = updatedContent)
                val updatedMessages = messages.copy(message = updatedMessage)

                state.copy(messages = updatedMessages)
            }

            else -> throw IllegalStateException("unknown step: '$step'")
        }

    override fun transformInvalidOptionStepMessage(state: DialogState): DialogState =
        when (val step = state.currentStep()) {
            PromisePaymentStep.WARNING -> state

            PromisePaymentStep.AMOUNT -> {
                val messages = state.messages!!

                val updatedText = messages.invalidOptionTemplateText?.format(state.amount(), AMOUNT_LOWER_BOUND, AMOUNT_UPPER_BOUND)!!
                val invalidOptionMessage = messages.invalidOptionMessage!!

                val updatedContent = invalidOptionMessage.content?.copy(text = updatedText)
                val updatedMessage = invalidOptionMessage.copy(content = updatedContent)
                val updatedMessages = messages.copy(invalidOptionMessage = updatedMessage)

                state.copy(messages = updatedMessages)
            }

            else -> throw IllegalStateException("unknown step: '$step'")
        }

    private fun createWarningStepMessages() =
        DialogState.MessageContainer(
            message = responseMessageService.clientPromisePaymentWarningMessage(),
            invalidOptionMessage = responseMessageService.clientPromisePaymentInvalidWarningMessage()
        )

    private fun DialogState.recommendedAmount(): Int =
        (this.stepData as PromisePaymentAmountStepData).recommendedAmount!!

    private fun DialogState.amount(): Int =
        (this.stepData as PromisePaymentAmountStepData).amount!!

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

    private fun getActualRecommendedPayment(user: UserEntity): Mono<Int> =
        recommendedPaymentService.getActual(user.agrmId!!)
            .map { AmountUtils.integerRound(it) }

    private fun addAmountToOptions(state: DialogState): DialogState {
        val options = state.options as PromisePaymentOptions
        options.amount = state.amount()
        return state.copy(options = options)
    }

    private fun updateAmountStepData(state: DialogState, option: String): PromisePaymentAmountStepData {
        val stepData = state.stepData as PromisePaymentAmountStepData
        return stepData.copy(amount = doUpdateAmount(state.amount(), option))
    }
}