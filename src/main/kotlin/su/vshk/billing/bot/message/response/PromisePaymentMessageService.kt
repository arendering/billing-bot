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
}
