package su.vshk.billing.bot.message.response

import org.springframework.context.MessageSource
import org.springframework.stereotype.Service
import su.vshk.billing.bot.dao.model.Command
import su.vshk.billing.bot.message.HtmlMarkupFormatter
import su.vshk.billing.bot.message.dto.ResponseMessageItem
import su.vshk.billing.bot.service.dto.LoginMessageDto

@Service
class LoginMessageService(
    private val messageSource: MessageSource
): ResponseMessageService(messageSource) {

    /**
     * Приглашение ввести логин.
     */
    fun enterLogin(): ResponseMessageItem =
        ResponseMessageItem(
            content = ResponseMessageItem.Content(
                text = HtmlMarkupFormatter()
                    .addText(text = getText("login.welcome"))
                    .addText(text = getText("login.username"))
                    .build(),
                buttons = emptyList()
            ),
            meta = ResponseMessageItem.Meta().sendMessage()
        )

    /**
     * Приглашение ввести пароль.
     */
    fun enterPassword(): ResponseMessageItem =
        ResponseMessageItem(
            content = ResponseMessageItem.Content(
                text = getText("login.password"),
                buttons = emptyList()
            ),
            meta = ResponseMessageItem.Meta().sendMessage()
        )

    /**
     * Некорректные имя пользователя/пароль.
     */
    fun showInvalidCredentials(loginMessageIds: LoginMessageDto): ResponseMessageItem =
        ResponseMessageItem(
            content = ResponseMessageItem.Content(
                text = getText("login.invalid.creds"),
                buttons = registerButton()
            ),
            meta = createFinalMeta(loginMessageIds)
        )

    /**
     * Непредвиденная ошибка.
     */
    fun showUnexpectedError(loginMessageIds: LoginMessageDto): ResponseMessageItem =
        ResponseMessageItem(
            content = ResponseMessageItem.Content(
                text = getText("common.error"),
                buttons = registerButton()
            ),
            meta = createFinalMeta(loginMessageIds).notifyErrorGroup()
        )

    /**
     * Показывает главное меню при успешной регистрации.
     */
    fun showMainMenu(loginMessageIds: LoginMessageDto): ResponseMessageItem =
        ResponseMessageItem(
            content = mainMenuContent(),
            meta = createFinalMeta(loginMessageIds)
        )

    private fun registerButton(): List<List<ResponseMessageItem.InlineKeyboardItem>> =
        listOf(
            listOf(
                ResponseMessageItem.InlineKeyboardItem(
                    label = getText("login.register"),
                    callbackData = Command.LOGIN.value
                )
            )
        )

    /**
     * Мета-информация для финального сообщения.
     */
    private fun createFinalMeta(loginMessageIds: LoginMessageDto): ResponseMessageItem.Meta =
        ResponseMessageItem.Meta()
            .deleteMessages(loginMessageIds.deleteMessageIds)
            .let { m ->
                loginMessageIds.editMessageId
                    ?.let { id -> m.editMessage(id) }
                    ?: m.sendMessage()
            }

}