package su.vshk.billing.bot.dao.repository

import su.vshk.billing.bot.dao.model.UserEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import su.vshk.billing.bot.dao.model.EnabledNotificationUserDto

@Repository
interface UserRepository: JpaRepository<UserEntity, Long> {

    @Query(
        """
            SELECT NEW su.vshk.billing.bot.dao.model.EnabledNotificationUserDto(
                u.telegramId,
                u.userId,
                u.agreementId,
                p.notificationType
            )
            FROM PaymentNotificationEntity p
            LEFT JOIN UserEntity u
            ON p.telegramId = u.telegramId
        """
    )
    fun findUsersEnabledNotification(): List<EnabledNotificationUserDto>
}