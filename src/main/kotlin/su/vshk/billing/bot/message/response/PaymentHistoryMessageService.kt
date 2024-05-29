package su.vshk.billing.bot.message.response

import org.springframework.context.MessageSource
import org.springframework.stereotype.Service
import su.vshk.billing.bot.dialog.option.PaymentHistoryAvailableOptions
import su.vshk.billing.bot.message.HtmlMarkupFormatter
import su.vshk.billing.bot.message.TextType
import su.vshk.billing.bot.message.dto.ResponseMessageItem
import su.vshk.billing.bot.service.dto.PaymentHistoryDto
import su.vshk.billing.bot.util.AmountUtils

@Service
class PaymentHistoryMessageService(
    private val messageSource: MessageSource
): ResponseMessageService(messageSource) {

    /**
     * Показать периоды времени, за которые можно посмотреть историю.
     */
    fun showHistoryPeriods(): ResponseMessageItem =
        createEditMessage(
            content = ResponseMessageItem.Content(
                text = getText("payment.history.period"),
                buttons = listOf(
                    listOf(
                        ResponseMessageItem.InlineKeyboardItem(
                            label = getText("payment.history.period.one.month"),
                            callbackData = PaymentHistoryAvailableOptions.PERIOD_ONE_MONTH
                        ),
                        ResponseMessageItem.InlineKeyboardItem(
                            label = getText("payment.history.period.three.months"),
                            callbackData = PaymentHistoryAvailableOptions.PERIOD_THREE_MONTHS
                        ),
                        ResponseMessageItem.InlineKeyboardItem(
                            label = getText("payment.history.period.six.months"),
                            callbackData = PaymentHistoryAvailableOptions.PERIOD_SIX_MONTHS
                        )
                    ),
                    listOf(getCancelKeyboardItem())
                )
            )
        )

    /**
     * Показать историю платежей.
     */
    fun showHistory(dto: PaymentHistoryDto): ResponseMessageItem =
        createEditMessage(
            content = ResponseMessageItem.Content(
                text = HtmlMarkupFormatter()
                    .addText(text = getText("payment.history.final.header", dto.dateFrom, dto.dateTo), textType = TextType.BOLD_UNDERLINED)
                    .addLineBreak()
                    .addHistory(dto.payments)
                    .build(),
                buttons = listOf(listOf(getBackToMainMenuKeyboardItem()))
            )
        )

    private fun HtmlMarkupFormatter.addHistory(payments: List<PaymentHistoryDto.PaymentDto>?): HtmlMarkupFormatter =
        if (payments.isNullOrEmpty()) {
            this.addText(text = getText("payment.history.final.empty"))
        } else {
            payments
                .withIndex()
                .forEach { (i, payment) ->
                    this
                        .addEntry(
                            key = getText("payment.history.final.date.key"),
                            keyType = TextType.PLAIN,
                            value = payment.date
                        )
                        .addEntry(
                            key = getText("payment.history.final.time.key"),
                            keyType = TextType.PLAIN,
                            value = payment.time
                        )
                        .addEntry(
                            key = getText("payment.history.final.id.key"),
                            keyType = TextType.PLAIN,
                            value = payment.id
                        )
                        .addEntry(
                            key = getText("payment.history.final.amount.key"),
                            keyType = TextType.PLAIN,
                            value = payment.amount?.let { AmountUtils.formatAmount(it) }
                        )
                        .addEntry(
                            key = getText("payment.history.final.manager.key"),
                            keyType = TextType.PLAIN,
                            value = payment.manager
                        )
                    if (payments.lastIndex != i) {
                        this.addLineBreak()
                    }
                }

            this
        }
}