package su.vshk.billing.bot.dao.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import su.vshk.billing.bot.dao.model.PaymentNotificationMessageEntity

@Repository
interface PaymentNotificationMessageRepository: JpaRepository<PaymentNotificationMessageEntity, Long>