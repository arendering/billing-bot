package su.vshk.billing.bot.service.dto

data class LoginMessageDto(
    /**
     * Id сообщения, которое будет отредактировано.
     */
    val editMessageId: Int? = null,

    /**
     * Id сообщений, которые будут удалены
     */
    val deleteMessageIds: List<Int> = emptyList()
)
