package su.vshk.billing.bot.message.response

import org.springframework.context.MessageSource
import org.springframework.stereotype.Service
import su.vshk.billing.bot.dao.model.Command
import su.vshk.billing.bot.dialog.option.ExitAvailableOptions
import su.vshk.billing.bot.message.dto.ResponseMessageItem

@Service
class LogoutMessageService(
    private val messageSource: MessageSource
): ResponseMessageService(messageSource) {

    /**
     * Показывает предупреждение.
     */
    fun showWarning(): ResponseMessageItem =
        createEditMessage(
            content = ResponseMessageItem.Content(
                text = getText("logout.warning"),
                buttons = listOf(
                    listOf(
                        ResponseMessageItem.InlineKeyboardItem(
                            label = getText("logout.warning.button.label"),
                            callbackData = ExitAvailableOptions.YES
                        )
                    ),
                    listOf(
                        getCancelKeyboardItem()
                    )
                )
            )
        )

    /**
     * Сообщает об успешном выходе.
     */
    fun showSuccessLogout(paymentNotificationMessageId: Int?): ResponseMessageItem =
        ResponseMessageItem(
            content = ResponseMessageItem.Content(
                text = getText("logout.success"),
                buttons = listOf(
                    listOf(
                        ResponseMessageItem.InlineKeyboardItem(
                            label = getText("login.register"),
                            callbackData = Command.LOGIN.value
                        )
                    )
                )
            ),
            meta = ResponseMessageItem.Meta()
                .editMessage()
                .deleteMessages(listOfNotNull(paymentNotificationMessageId))
        )
}