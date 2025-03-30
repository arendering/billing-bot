package su.vshk.billing.bot.message.response

import org.springframework.context.MessageSource
import org.springframework.stereotype.Service
import su.vshk.billing.bot.message.HtmlMarkupFormatter
import su.vshk.billing.bot.message.TextType
import su.vshk.billing.bot.message.dto.ResponseMessageItem

@Service
class YookassaPaymentMessageService(
    private val messageSource: MessageSource
): ResponseMessageService(messageSource) {

    /**
     * Показать сообщение с предупреждением, что нужно заполнить эл. почту и телефон в договоре.
     */
    fun showInvalidCustomerMessage(): ResponseMessageItem =
        createEditMessage(
            content = ResponseMessageItem.Content(
                text = HtmlMarkupFormatter()
                    .addText(getText("yookassa.payment.invalid.customer.main"))
                    .addLineBreak()
                    .addHref(
                        text = getText("yookassa.payment.invalid.customer.label"),
                        href = getText("yookassa.payment.invalid.customer.link")
                    )
                    .build(),
                buttons = listOf(listOf(getBackToMainMenuKeyboardItem()))
            )
        )

    fun getPaymentDescription(agreementNumber: String): String =
        getText("yookassa.payment.description").format(agreementNumber)

    fun getPaymentReceiptDescription(agreementNumber: String): String =
        getText("yookassa.payment.receipt.description").format(agreementNumber)

    /**
     * Показать сообщение со ссылкой для оплаты.
     */
    fun showPaymentLinkMessage(confirmationUrl: String): ResponseMessageItem =
        createEditMessage(
            content = ResponseMessageItem.Content(
                text = HtmlMarkupFormatter()
                    .addText(text = getText("yookassa.payment.confirmation.main"))
                    .addLineBreak()
                    .addText(text = getText("yookassa.payment.confirmation.warning.1"), textType = TextType.ITALIC)
                    .addLineBreak()
                    .addText(text = getText("yookassa.payment.confirmation.warning.2"), textType = TextType.ITALIC)
                    .addLineBreak()
                    .addHref(text = getText("yookassa.payment.confirmation.url.label"), href = confirmationUrl)
                    .addLineBreak()
                    .addText(text = getText("yookassa.payment.confirmation.footer"))
                    .build(),
                buttons = listOf(listOf(getBackToMainMenuKeyboardItem()))
            )
        )
}