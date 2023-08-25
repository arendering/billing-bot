package su.vshk.billing.bot.service.dto

data class LoginInputDto(
    /**
     * Является ли первое сообщение текстовым
     */
    val isFirstMessageTextInput: Boolean = false,
    /**
     * Является ли первое сообщение вводом с кнопки
     */
    val isFirstMessageButtonInput: Boolean = false,
    /**
     * Список всех id сообщений из стартового диалога
     */
    val messageIds: List<Int> = emptyList()
)
