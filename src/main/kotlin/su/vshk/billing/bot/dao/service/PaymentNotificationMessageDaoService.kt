package su.vshk.billing.bot.dao.service

import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.kotlin.core.publisher.toMono
import su.vshk.billing.bot.dao.model.PaymentNotificationMessageEntity
import su.vshk.billing.bot.dao.repository.PaymentNotificationMessageRepository
import java.util.*

@Service
class PaymentNotificationMessageDaoService(
    private val paymentNotificationMessageRepository: PaymentNotificationMessageRepository
) {

    fun saveAll(entities: List<PaymentNotificationMessageEntity>): Mono<List<PaymentNotificationMessageEntity>> =
        Mono.fromCallable {
            paymentNotificationMessageRepository.saveAll(entities)
        }.subscribeOn(Schedulers.boundedElastic())

    fun removeAllSafe(): Mono<List<PaymentNotificationMessageEntity>> =
        Mono.fromCallable { paymentNotificationMessageRepository.findAll() }
            .subscribeOn(Schedulers.boundedElastic())
            .flatMap { entities ->
                if (entities.isEmpty()) {
                    entities.toMono()
                } else {
                    Mono.fromCallable { paymentNotificationMessageRepository.deleteAll() }
                        .subscribeOn(Schedulers.boundedElastic())
                        .thenReturn(entities)
                }
            }

    fun removeByIdSafe(telegramId: Long): Mono<Optional<PaymentNotificationMessageEntity>> =
        Mono.fromCallable { paymentNotificationMessageRepository.findById(telegramId) }
            .subscribeOn(Schedulers.boundedElastic())
            .flatMap { entityOpt ->
                if (entityOpt.isEmpty) {
                    entityOpt.toMono()
                } else {
                    Mono.fromCallable { paymentNotificationMessageRepository.deleteById(telegramId) }
                        .subscribeOn(Schedulers.boundedElastic())
                        .thenReturn(entityOpt)
                }
            }
}