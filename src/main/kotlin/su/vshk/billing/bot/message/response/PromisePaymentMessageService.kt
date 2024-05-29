package su.vshk.billing.bot.message.response

import org.springframework.context.MessageSource
import org.springframework.stereotype.Service
import su.vshk.billing.bot.dialog.option.PromisePaymentAvailableOptions
import su.vshk.billing.bot.message.HtmlMarkupFormatter
import su.vshk.billing.bot.message.TextType
import su.vshk.billing.bot.message.dto.ResponseMessageItem

@Service
class PromisePaymentMessageService(
    private val messageSource: MessageSource
): ResponseMessageService(messageSource) {

    /**
     * Показывает предупреждение.
     */
    fun showWarning(): ResponseMessageItem =
        createEditMessage(
            content = ResponseMessageItem.Content(
                text = HtmlMarkupFormatter()
                    .addText(text = getText("promise.payment.warning.header"))
                    .addText(text = getText("promise.payment.warning.body"))
                    .addLineBreak()
                    .addText(
                        text = getText("promise.payment.warning.footer"),
                        textType = TextType.ITALIC
                    )
                    .build(),
                buttons = listOf(
                    listOf(
                        ResponseMessageItem.InlineKeyboardItem(
                            label = getText("promise.payment.warning.approve.button.label"),
                            callbackData = PromisePaymentAvailableOptions.WARNING_APPROVE
                        )
                    ),
                    listOf(getCancelKeyboardItem())
                )
            )
        )

    /**
     * Показывает калькулятор для выбора суммы.
     */
    fun showCalculator(amount: Int): ResponseMessageItem =
        createEditMessage(
            content = ResponseMessageItem.Content(
                text = calculatorBaseFormatter(amount).build(),
                buttons = calculatorButtons()
            )
        )

    /**
     * Показывает калькулятор со слишком низкой суммой.
     */
    fun showCalculatorWithTooLowAmount(amount: Int, lowerBound: Int): ResponseMessageItem =
        createEditMessage(
            content = ResponseMessageItem.Content(
                text = calculatorBaseFormatter(amount)
                    .addFormattedText(
                        text = getText("promise.payment.amount.footer.too.low.template"),
                        textType = TextType.ITALIC,
                        value = "$lowerBound $RUB",
                        valueType = TextType.PLAIN
                    )
                    .build(),
                buttons = calculatorButtons()
            )
        )

    /**
     * Показывает калькулятор со слишком высокой суммой.
     */
    fun showCalculatorWithTooHighAmount(amount: Int, upperBound: Int): ResponseMessageItem =
        createEditMessage(
            content = ResponseMessageItem.Content(
                text = calculatorBaseFormatter(amount)
                    .addFormattedText(
                        text = getText("promise.payment.amount.footer.too.high.template"),
                        textType = TextType.ITALIC,
                        value = "$upperBound $RUB",
                        valueType = TextType.PLAIN
                    )
                    .build(),
                buttons = calculatorButtons()
            )
        )


    /**
     * Сообщает о превышении задолженности на счете.
     */
    fun showDebtsOverdueError(): ResponseMessageItem =
        createEditMessage(
            content = ResponseMessageItem.Content(
                text = getText("promise.payment.final.debts.overdue"),
                buttons = listOf(listOf(getBackToMainMenuKeyboardItem()))
            )
        )

    /**
     * Сообщает о просрочке последнего платежа.
     */
    fun showLastPaymentOverdueError(): ResponseMessageItem =
        createEditMessage(
            content = ResponseMessageItem.Content(
                text = getText("promise.payment.final.overdue"),
                buttons = listOf(listOf(getBackToMainMenuKeyboardItem()))
            )
        )

    /**
     * Сообщает о том, что обещанный платеж уже подключен.
     */
    fun showPaymentAlreadyAssignedError(): ResponseMessageItem =
        createEditMessage(
            content = ResponseMessageItem.Content(
                text = getText("promise.payment.final.assigned"),
                buttons = listOf(listOf(getBackToMainMenuKeyboardItem()))
            )
        )

    /**
     * Сообщает о возникновении ошибки при подключении обещаннго платежа.
      */
    fun showPaymentGenericError(): ResponseMessageItem =
        createEditMessage(
            content = ResponseMessageItem.Content(
                text = getText("promise.payment.final.error"),
                buttons = listOf(listOf(getBackToMainMenuKeyboardItem()))
            )
        )

    /**
     * Сообщает об успешном подключении обещанного платежа.
     */
    fun showSuccessfullyAssignedPayment(): ResponseMessageItem =
        createEditMessage(
            content = ResponseMessageItem.Content(
                text = getText("promise.payment.final.success"),
                buttons = listOf(listOf(getBackToMainMenuKeyboardItem()))
            )
        )

    private fun calculatorButtons(): List<List<ResponseMessageItem.InlineKeyboardItem>> =
        listOf(
            listOf(
                ResponseMessageItem.InlineKeyboardItem(
                    label = getText("promise.payment.amount.calculator.plus.one") + " $RUB",
                    callbackData = PromisePaymentAvailableOptions.AMOUNT_PLUS_ONE
                ),
                ResponseMessageItem.InlineKeyboardItem(
                    label = getText("promise.payment.amount.calculator.minus.one") + " $RUB",
                    callbackData = PromisePaymentAvailableOptions.AMOUNT_MINUS_ONE
                )
            ),
            listOf(
                ResponseMessageItem.InlineKeyboardItem(
                    label = getText("promise.payment.amount.calculator.plus.twenty.five") + " $RUB",
                    callbackData = PromisePaymentAvailableOptions.AMOUNT_PLUS_TWENTY_FIVE
                ),
                ResponseMessageItem.InlineKeyboardItem(
                    label = getText("promise.payment.amount.calculator.minus.twenty.five") + " $RUB",
                    callbackData = PromisePaymentAvailableOptions.AMOUNT_MINUS_TWENTY_FIVE
                )
            ),
            listOf(
                ResponseMessageItem.InlineKeyboardItem(
                    label = getText("promise.payment.amount.calculator.plus.one.hundred") + " $RUB",
                    callbackData = PromisePaymentAvailableOptions.AMOUNT_PLUS_ONE_HUNDRED
                ),
                ResponseMessageItem.InlineKeyboardItem(
                    label = getText("promise.payment.amount.calculator.minus.one.hundred") + " $RUB",
                    callbackData = PromisePaymentAvailableOptions.AMOUNT_MINUS_ONE_HUNDRED
                )
            ),
            listOf(
                ResponseMessageItem.InlineKeyboardItem(
                    label = getText("promise.payment.amount.calculator.submit"),
                    callbackData = PromisePaymentAvailableOptions.AMOUNT_SUBMIT
                ),
            ),
            listOf(
                ResponseMessageItem.InlineKeyboardItem(
                    label = getText("common.cancel.button.label"),
                    callbackData = PromisePaymentAvailableOptions.CANCEL_AMOUNT_STEP
                )
            )
        )

    private fun calculatorBaseFormatter(amount: Int): HtmlMarkupFormatter =
        HtmlMarkupFormatter()
            .addText(text = getText("promise.payment.amount.header"))
            .addLineBreak()
            .addEntry(
                key = getText("promise.payment.amount.key"),
                keyType = TextType.BOLD_UNDERLINED,
                value = "$amount $RUB",
                valueType = TextType.BOLD
            )
}
