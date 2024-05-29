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

    @Column(name = "user_id")
    val userId: Long? = null,

    @Column(name = "login")
    val login: String? = null,

    @Column(name = "agreement_id")
    val agreementId: Long? = null
)