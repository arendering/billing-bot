package su.vshk.billing.bot.dao.service

import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import su.vshk.billing.bot.dao.model.PaymentNotificationEntity
import su.vshk.billing.bot.dao.repository.PaymentNotificationRepository
import java.util.*

@Service
class PaymentNotificationDaoService(
    private val paymentNotificationRepository: PaymentNotificationRepository
) {

    fun findById(telegramId: Long): Mono<Optional<PaymentNotificationEntity>> =
        Mono.fromCallable {
            paymentNotificationRepository.findById(telegramId)
        }.subscribeOn(Schedulers.boundedElastic())

    fun save(entity: PaymentNotificationEntity): Mono<PaymentNotificationEntity> =
        Mono.fromCallable {
            paymentNotificationRepository.save(entity)
        }.subscribeOn(Schedulers.boundedElastic())

    fun deleteById(telegramId: Long): Mono<Long> =
        Mono.fromCallable {
            paymentNotificationRepository.deleteById(telegramId)
            telegramId
        }.subscribeOn(Schedulers.boundedElastic())
}