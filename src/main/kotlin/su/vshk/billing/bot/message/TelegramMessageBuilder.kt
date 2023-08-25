package su.vshk.billing.bot.message

import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import su.vshk.billing.bot.message.dto.ResponseMessageItem

class TelegramMessageBuilder private constructor() {
    companion object {
        private const val PARSE_MODE = "HTML"

        /**
         * Создает новое ответное сообщение.
         */
        fun createMessage(telegramId: Long, content: ResponseMessageItem.Content): SendMessage =
            SendMessage()
                .also {
                    it.chatId = telegramId.toString()
                    it.text = content.text!!
                    it.replyMarkup = createKeyboard(content.buttons)
                    it.parseMode = PARSE_MODE
                }

        /**
         * Редактирует исходное сообщение.
         */
        fun editMessage(telegramId: Long, messageId: Int, content: ResponseMessageItem.Content): EditMessageText =
            EditMessageText()
                .also {
                    it.chatId = telegramId.toString()
                    it.text = content.text!!
                    it.replyMarkup = createKeyboard(content.buttons)
                    it.messageId = messageId
                    it.parseMode = PARSE_MODE
                }

        /**
         * Удаляет сообщение.
         */
        fun deleteMessage(telegramId: Long, messageId: Int): DeleteMessage =
            DeleteMessage()
                .also {
                    it.chatId = telegramId.toString()
                    it.messageId = messageId
                }

        private fun createKeyboard(buttons: List<List<ResponseMessageItem.InlineKeyboardItem>>): InlineKeyboardMarkup? {
            if (buttons.isEmpty() || buttons.all { it.isEmpty() }) {
                return null
            }

            return buttons
                .map { convertButtonRow(it) }
                .let { keyboard ->
                    InlineKeyboardMarkup()
                        .also { it.keyboard = keyboard }
                }
        }

        private fun convertButtonRow(buttonRow: List<ResponseMessageItem.InlineKeyboardItem>): List<InlineKeyboardButton> =
            buttonRow
                .map { button ->
                    InlineKeyboardButton()
                        .also {
                            it.text = button.label
                            it.callbackData = button.callbackData
                        }
                }
    }
}