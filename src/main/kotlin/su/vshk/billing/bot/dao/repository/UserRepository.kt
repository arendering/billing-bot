package su.vshk.billing.bot.dao.repository

import su.vshk.billing.bot.dao.model.UserEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserRepository: JpaRepository<UserEntity, Long> {
    fun findByPaymentNotificationEnabledTrue(): List<UserEntity>
}