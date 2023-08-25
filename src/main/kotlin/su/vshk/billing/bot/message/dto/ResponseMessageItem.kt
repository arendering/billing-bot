package su.vshk.billing.bot.message.dto

data class ResponseMessageItem(
    /**
     * Контент сообщения (текст, кнопки)
     */
    val content: Content? = null,

    /**
     * Метаинформация для отправки ответа
     */
    val meta: Meta
) {

    fun setSendMessageId(messageId: Int): ResponseMessageItem {
        val updatedSendMessageMeta = this.meta.sendMessage.copy(messageId = messageId)
        val updatedMeta = this.meta.copy(sendMessage = updatedSendMessageMeta)
        return this.copy(meta = updatedMeta)
    }

    fun setEditMessageId(messageId: Int): ResponseMessageItem {
        val updatedEditMessageMeta = this.meta.editMessage.copy(messageId = messageId)
        val updatedMeta = this.meta.copy(editMessage = updatedEditMessageMeta)
        return this.copy(meta = updatedMeta)
    }

    data class Meta(
        val sendMessage: SendMessageMeta = SendMessageMeta(),
        val editMessage: EditMessageMeta = EditMessageMeta(),
        val deleteMessages: DeleteMessagesMeta = DeleteMessagesMeta(),
        val notifyErrorGroup: NotifyErrorGroupMeta = NotifyErrorGroupMeta()
    ) {
        fun sendMessage(): Meta =
            this.copy(sendMessage = SendMessageMeta(active = true))

        fun editMessage(messageId: Int? = null): Meta =
            this.copy(editMessage = EditMessageMeta(active = true, messageId = messageId))

        fun deleteMessages(messageIds: List<Int>): Meta =
            this.copy(deleteMessages = DeleteMessagesMeta(active = true, messageIds = messageIds))

        fun notifyErrorGroup(): Meta =
            this.copy(notifyErrorGroup = NotifyErrorGroupMeta(active = true))
    }

    data class SendMessageMeta(
        /**
         * Флаг, означающий, что нужно отправить новое сообщение в ответе
         */
        val active: Boolean = false,
        /**
         * Id отправленного сообщения (заполняется после отправки ответа ботом)
         */
        val messageId: Int? = null
    )

    data class EditMessageMeta(
        /**
         * Флаг, означающий, что ответом является редактирование сообщения
         */
        val active: Boolean = false,
        /**
         * Id сообщения для редактирования (если == null, то редактируется сообщение из запроса)
         */
        val messageId: Int? = null
    )

    data class DeleteMessagesMeta(
        /**
         * Флаг, означающий, что ответом является удаление сообщений
         */
        val active: Boolean = false,
        /**
         * Id сообщений для удаления
         */
        val messageIds: List<Int> = emptyList()
    )

    data class NotifyErrorGroupMeta(
        /**
         * Флаг, означающий, что нужно отправить сообщение в группу с ошибками
         */
        val active: Boolean = false
    )

    data class Content(
        val text: String?,
        val buttons: List<List<InlineKeyboardItem>>
    )

    data class InlineKeyboardItem(
        val label: String,
        val callbackData: String
    )
}
