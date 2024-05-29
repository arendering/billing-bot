package su.vshk.billing.bot.dao.model

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "payment_notifications")
data class PaymentNotificationEntity(

    @Id
    @Column(name = "telegram_id")
    val telegramId: Long,

    @Column(name = "notification_type")
    val notificationType: String
) {
    fun isSingleType(): Boolean =
        notificationType == PaymentNotificationType.SINGLE

    fun isAllType(): Boolean =
        notificationType == PaymentNotificationType.ALL
}
