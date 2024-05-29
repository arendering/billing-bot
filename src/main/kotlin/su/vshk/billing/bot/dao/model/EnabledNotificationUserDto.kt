package su.vshk.billing.bot.dao.model

data class EnabledNotificationUserDto(
    val telegramId: Long,
    val userId: Long,
    val agreementId: Long,
    val notificationType: String
)
