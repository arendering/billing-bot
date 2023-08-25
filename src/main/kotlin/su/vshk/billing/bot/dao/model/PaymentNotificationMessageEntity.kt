package su.vshk.billing.bot.dao.model

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "payment_notification_messages")
data class PaymentNotificationMessageEntity(
    @Id
    @Column(name = "telegram_id")
    val telegramId: Long,

    @Column(name = "message_id")
    val messageId: Int
)
