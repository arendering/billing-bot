package su.vshk.billing.bot.message.dto

data class RequestMessageItem(
    /**
     * Ввод текста.
     */
    val isTextUpdate: Boolean,

    /**
     * Ввод по нажатию на кнопку.
     */
    val isButtonUpdate: Boolean,

    /**
     * Telegram id пользователя.
     */
    val chatId: Long,

    /**
     * Пользовательский ввод (набранный текст или данные с кнопки).
     */
    val input: String,

    /**
     * Id сообщения, которое отправил пользователь или на котором нажал кнопку.
     */
    val messageId: Int
)
