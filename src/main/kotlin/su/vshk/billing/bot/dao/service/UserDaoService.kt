package su.vshk.billing.bot.dao.service

import su.vshk.billing.bot.dao.model.UserEntity
import su.vshk.billing.bot.dao.repository.UserRepository
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.util.*

@Service
class UserDaoService(
    private val userRepository: UserRepository
) {
    fun findUser(telegramId: Long): Mono<Optional<UserEntity>> =
        Mono.fromCallable {
            userRepository.findById(telegramId)
        }.subscribeOn(Schedulers.boundedElastic())

    fun saveUser(
        telegramId: Long,
        uid: Long,
        login: String,
        password: String,
        agrmId: Long
    ): Mono<UserEntity> =
        Mono.fromCallable {
            UserEntity(
                telegramId = telegramId,
                uid = uid,
                login = login,
                password = password,
                agrmId = agrmId,
                paymentNotificationEnabled = false
            ).let { userRepository.save(it) }
        }.subscribeOn(Schedulers.boundedElastic())

    fun updateUser(updated: UserEntity): Mono<UserEntity> =
        Mono.fromCallable {
            userRepository.save(updated)
        }.subscribeOn(Schedulers.boundedElastic())

    fun deleteUser(telegramId: Long): Mono<Unit> =
        Mono.fromCallable {
            userRepository.deleteById(telegramId)
        }.subscribeOn(Schedulers.boundedElastic())

    fun findByPaymentNotificationEnabledTrue(): Mono<List<UserEntity>> =
        Mono.fromCallable {
            userRepository.findByPaymentNotificationEnabledTrue()
        }.subscribeOn(Schedulers.boundedElastic())
}