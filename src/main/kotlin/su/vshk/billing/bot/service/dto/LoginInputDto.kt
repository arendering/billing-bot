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
) {
    fun addMessageId(messageId: Int): LoginInputDto {
        val ids = this.messageIds
        return if (ids.contains(messageId)) {
            this
        } else {
            val updatedIds = ids.plus(messageId)
            this.copy(messageIds = updatedIds)
        }
    }
}
