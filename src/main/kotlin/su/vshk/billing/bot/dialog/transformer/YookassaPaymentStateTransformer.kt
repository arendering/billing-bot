package su.vshk.billing.bot.dialog.transformer

import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import su.vshk.billing.bot.dao.model.Command
import su.vshk.billing.bot.dao.model.UserEntity
import su.vshk.billing.bot.dialog.dto.DialogState
import su.vshk.billing.bot.dialog.option.YookassaPaymentOptions
import su.vshk.billing.bot.dialog.step.YookassaPaymentStep
import su.vshk.billing.bot.message.dto.RequestMessageItem
import su.vshk.billing.bot.message.dto.ResponseMessageItem
import su.vshk.billing.bot.message.response.YookassaPaymentMessageService
import su.vshk.billing.bot.service.AccountService
import su.vshk.billing.bot.service.CalculatorService
import su.vshk.billing.bot.service.RecommendedPaymentService
import su.vshk.billing.bot.util.AmountUtils
import su.vshk.billing.bot.util.PhoneNormalizer
import su.vshk.billing.bot.web.dto.manager.Account
import su.vshk.billing.bot.web.dto.yookassa.YookassaPaymentReceiptCustomer

@Component
class YookassaPaymentStateTransformer(
    private val recommendedPaymentService: RecommendedPaymentService,
    private val calculatorService: CalculatorService,
    private val yookassaPaymentMessageService: YookassaPaymentMessageService,
    private val accountService: AccountService
): DialogStateTransformer {

    override fun getCommand(): Command =
        Command.YOOKASSA_PAYMENT

    override fun initializeState(request: RequestMessageItem, user: UserEntity): Mono<DialogState> =
        getAccount(user)
            .flatMap { account ->
                if (account.isValidCustomerInfo()) {
                    getActualRecommendedPayment(user)
                        .map { recommendedPayment ->
                            val responseMessage = initializeCalculatorState(telegramId = user.telegramId, amount = recommendedPayment)
                            DialogState(
                                command = getCommand(),
                                options = YookassaPaymentOptions(customer = account.resolveCustomer()),
                                steps = listOf(YookassaPaymentStep.AMOUNT),
                                response = DialogState.Response.next(responseMessage)
                            )
                        }
                } else {
                    DialogState(
                        command = getCommand(),
                        response = DialogState.Response.cancel(yookassaPaymentMessageService.showInvalidCustomerMessage())
                    ).toMono()
                }
            }

    override fun processOption(request: RequestMessageItem, user: UserEntity, state: DialogState): Mono<DialogState> =
        when (val step = state.currentStep()) {
            YookassaPaymentStep.AMOUNT -> processAmountStep(user, state, request.input).toMono()
            else -> throw IllegalStateException("unknown step: '$step'")
        }

    private fun getAccount(user: UserEntity): Mono<Account> =
        accountService.getAccount(user.userId!!)
            .map { it.account ?: throw RuntimeException("account not found for user $user") }

    private fun getActualRecommendedPayment(user: UserEntity): Mono<Int> =
        recommendedPaymentService.getActual(user.agreementId!!)
            .map { AmountUtils.integerRound(it) }

    private fun initializeCalculatorState(telegramId: Long, amount: Int): ResponseMessageItem =
        calculatorService.initialize(telegramId = telegramId, amount = amount, command = getCommand())

    private fun processAmountStep(user: UserEntity, state: DialogState, option: String): DialogState {
        val dto = calculatorService.processOption(telegramId = user.telegramId, option = option)
        return when {
            dto.next -> state.stayCurrentStep(dto.response!!)
            dto.cancel -> state.cancel(dto.response!!)
            dto.finish -> {
                val updatedOptions = (state.options as YookassaPaymentOptions).copy(amount = dto.amount)
                state.finish(updatedOptions)
            }
            else -> throw RuntimeException("inconsistent calculator dto $dto")
        }
    }

    /**
     * Одно из полей recipient.customer.email или recipient.customer.mobile должно быть непустым (требование Юкассы)
     */
    private fun Account.isValidCustomerInfo(): Boolean =
        !this.email.isNullOrEmpty() || !this.mobile.isNullOrEmpty()

    private fun Account.resolveCustomer() =
        YookassaPaymentReceiptCustomer(
            email = if (this.email.isNullOrEmpty()) null else this.email,
            phone = PhoneNormalizer.normalizeForYookassa(this.mobile)
        )
}