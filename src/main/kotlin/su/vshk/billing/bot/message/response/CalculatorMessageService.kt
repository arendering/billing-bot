package su.vshk.billing.bot.message.response

import org.springframework.context.MessageSource
import org.springframework.stereotype.Service
import su.vshk.billing.bot.dao.model.CalculatorButton
import su.vshk.billing.bot.message.HtmlMarkupFormatter
import su.vshk.billing.bot.message.TextType
import su.vshk.billing.bot.message.dto.ResponseMessageItem
import su.vshk.billing.bot.util.AmountUtils
import java.math.BigDecimal

@Service
class CalculatorMessageService(
    private val messageSource: MessageSource
): ResponseMessageService(messageSource) {

    /**
     * Показать калькулятор.
     */
    fun showCalculator(amount: Int): ResponseMessageItem =
        createEditMessage(
            content = ResponseMessageItem.Content(
                text = getMainCalculatorFormatter(amount).build(),
                buttons = getCalculatorButtons()
            )
        )

    /**
     * Показать калькулятор с предупреждением, что значение суммы равно нулю (т.е не введено).
     */
    fun showEmptyAmountWarningCalculator(): ResponseMessageItem =
        createEditMessage(
            content = ResponseMessageItem.Content(
                text = getMainCalculatorFormatter(0)
                    .addText(
                        text = getText("calculator.too.low.warning"),
                        textType = TextType.ITALIC
                    )
                    .build(),
                buttons = getCalculatorButtons()
            )
        )

    /**
     * Показать калькулятор с предупреждением, что значение суммы превысило максимально допустимую сумму.
     */
    fun showMaxAmountWarningCalculator(amount: Int, maxLimit: Int): ResponseMessageItem =
        createEditMessage(
            content = ResponseMessageItem.Content(
                text = getMainCalculatorFormatter(amount)
                    .addFormattedText(
                        text = getText("calculator.too.high.warning.template"),
                        textType = TextType.ITALIC,
                        value = AmountUtils.formatAmount(amount = BigDecimal(maxLimit)),
                        valueType = TextType.PLAIN
                    )
                    .build(),
                buttons = getCalculatorButtons()
            )
        )

    private fun getMainCalculatorFormatter(amount: Int): HtmlMarkupFormatter =
        HtmlMarkupFormatter()
            .addText(text = getText("calculator.main"))
            .addLineBreak()
            .addEntry(
                key = getText("calculator.amount.key"),
                keyType = TextType.BOLD_UNDERLINED,
                value = AmountUtils.formatAmount(BigDecimal(amount)),
                valueType = TextType.BOLD
            )

    private fun getCalculatorButtons(): List<List<ResponseMessageItem.InlineKeyboardItem>> =
        listOf(
            listOf(
                ResponseMessageItem.InlineKeyboardItem(
                    label = getText("calculator.seven"),
                    callbackData = CalculatorButton.SEVEN
                ),
                ResponseMessageItem.InlineKeyboardItem(
                    label = getText("calculator.eight"),
                    callbackData = CalculatorButton.EIGHT
                ),
                ResponseMessageItem.InlineKeyboardItem(
                    label = getText("calculator.nine"),
                    callbackData = CalculatorButton.NINE
                )
            ),
            listOf(
                ResponseMessageItem.InlineKeyboardItem(
                    label = getText("calculator.four"),
                    callbackData = CalculatorButton.FOUR
                ),
                ResponseMessageItem.InlineKeyboardItem(
                    label = getText("calculator.five"),
                    callbackData = CalculatorButton.FIVE
                ),
                ResponseMessageItem.InlineKeyboardItem(
                    label = getText("calculator.six"),
                    callbackData = CalculatorButton.SIX
                )
            ),
            listOf(
                ResponseMessageItem.InlineKeyboardItem(
                    label = getText("calculator.one"),
                    callbackData = CalculatorButton.ONE
                ),
                ResponseMessageItem.InlineKeyboardItem(
                    label = getText("calculator.two"),
                    callbackData = CalculatorButton.TWO
                ),
                ResponseMessageItem.InlineKeyboardItem(
                    label = getText("calculator.three"),
                    callbackData = CalculatorButton.THREE
                )
            ),
            listOf(
                ResponseMessageItem.InlineKeyboardItem(
                    label = getText("calculator.erase"),
                    callbackData = CalculatorButton.ERASE
                ),
                ResponseMessageItem.InlineKeyboardItem(
                    label = getText("calculator.zero"),
                    callbackData = CalculatorButton.ZERO
                ),
                ResponseMessageItem.InlineKeyboardItem(
                    label = getText("calculator.clear"),
                    callbackData = CalculatorButton.CLEAR
                )
            ),
            listOf(
                ResponseMessageItem.InlineKeyboardItem(
                    label = getText("calculator.enter"),
                    callbackData = CalculatorButton.ENTER
                )
            ),
            listOf(
                ResponseMessageItem.InlineKeyboardItem(
                    label = getText("menu.back"),
                    callbackData = CalculatorButton.CANCEL_AMOUNT_STEP
                )
            )
        )
}