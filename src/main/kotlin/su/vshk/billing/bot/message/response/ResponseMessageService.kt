package su.vshk.billing.bot.message.response

import org.springframework.context.MessageSource
import su.vshk.billing.bot.dao.model.Command
import su.vshk.billing.bot.dialog.option.GenericAvailableOptions
import su.vshk.billing.bot.message.HtmlMarkupFormatter
import su.vshk.billing.bot.message.TextType
import su.vshk.billing.bot.message.dto.ResponseMessageItem
import java.util.*

abstract class ResponseMessageService(
    private val messageSource: MessageSource
) {
    private val locale = Locale("ru", "RU")

    fun showMainMenu() =
        createEditMessage(content = mainMenuContent())

    fun showGenericError(): ResponseMessageItem =
        ResponseMessageItem(
            content = ResponseMessageItem.Content(
                text = getText("common.error"),
                buttons = listOf(listOf(getBackToMainMenuKeyboardItem()))
            ),
            meta = ResponseMessageItem.Meta().editMessage().notifyErrorGroup()
        )

    fun deleteMessage(messageId: Int): ResponseMessageItem =
        ResponseMessageItem(
            meta = ResponseMessageItem.Meta().deleteMessages(listOf(messageId))
        )

    protected fun mainMenuContent(): ResponseMessageItem.Content =
        ResponseMessageItem.Content(
            text = HtmlMarkupFormatter()
                .addText(text = getText("menu.main.header"), textType = TextType.BOLD_ITALIC)
                .build(),
            buttons = listOf(
                listOf(agreementsKeyboardItem(), yookassaPaymentKeyboardItem()),
                listOf(paymentHistoryKeyboardItem(), promisePaymentKeyboardItem()),
                listOf(tariffsKeyboardItem(), notificationKeyboardItem()),
                listOf(contactsKeyboardItem(), exitKeyboardItem())
            )
        )

    protected fun createEditMessage(content: ResponseMessageItem.Content) =
        ResponseMessageItem(
            content = content,
            meta = ResponseMessageItem.Meta().editMessage()
        )

    protected fun getCancelKeyboardItem() =
        ResponseMessageItem.InlineKeyboardItem(
            label = getText("menu.back"),
            callbackData = GenericAvailableOptions.CANCEL
        )

    protected fun getBackToMainMenuKeyboardItem() =
        ResponseMessageItem.InlineKeyboardItem(
            label = getText("menu.back"),
            callbackData = Command.MENU.value
        )

    protected fun getText(code: String, vararg args: String): String =
        messageSource.getMessage(code, args, locale)

    private fun agreementsKeyboardItem() =
        ResponseMessageItem.InlineKeyboardItem(label = getText("menu.agreements"), callbackData = Command.AGREEMENTS.value)

    private fun yookassaPaymentKeyboardItem() =
        ResponseMessageItem.InlineKeyboardItem(label = getText("menu.yookassa.payment"), callbackData = Command.YOOKASSA_PAYMENT.value)

    private fun paymentHistoryKeyboardItem() =
        ResponseMessageItem.InlineKeyboardItem(label = getText("menu.payment.history"), callbackData = Command.PAYMENT_HISTORY.value)

    private fun promisePaymentKeyboardItem() =
        ResponseMessageItem.InlineKeyboardItem(label = getText("menu.promise.payment"), callbackData = Command.PROMISE_PAYMENT.value)

    private fun tariffsKeyboardItem() =
        ResponseMessageItem.InlineKeyboardItem(label = getText("menu.tariffs"), callbackData = Command.TARIFFS.value)

    private fun notificationKeyboardItem() =
        ResponseMessageItem.InlineKeyboardItem(label = getText("menu.notification"), callbackData = Command.NOTIFICATION.value)

    private fun contactsKeyboardItem() =
        ResponseMessageItem.InlineKeyboardItem(label = getText("menu.contacts"), callbackData = Command.CONTACTS.value)

    private fun exitKeyboardItem() =
        ResponseMessageItem.InlineKeyboardItem(label = getText("menu.exit"), callbackData = Command.EXIT.value)
}