package su.vshk.billing.bot.dao.model

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "users")
data class UserEntity(

    @Id
    @Column(name = "telegram_id")
    val telegramId: Long,

    @Column(name = "uid")
    val uid: Long? = null,

    @Column(name = "login")
    val login: String? = null,

    @Column(name = "password")
    val password: String? = null,

    @Column(name = "agrm_id")
    val agrmId: Long? = null,

    @Column(name = "payment_notification_enabled")
    val paymentNotificationEnabled: Boolean? = null
)