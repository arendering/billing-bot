package su.vshk.billing.bot.dao.service

import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.kotlin.core.publisher.toMono
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

    fun removeByIdSafe(telegramId: Long): Mono<Optional<PaymentNotificationEntity>> =
        findById(telegramId)
            .flatMap { entityOpt ->
                if (entityOpt.isPresent) {
                    Mono.fromCallable {
                        paymentNotificationRepository.deleteById(telegramId)
                        entityOpt
                    }.subscribeOn(Schedulers.boundedElastic())
                } else {
                    Optional.empty<PaymentNotificationEntity>().toMono()
                }
            }
}