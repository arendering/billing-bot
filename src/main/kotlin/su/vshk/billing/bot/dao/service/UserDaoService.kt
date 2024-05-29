package su.vshk.billing.bot.dao.service

import su.vshk.billing.bot.dao.model.UserEntity
import su.vshk.billing.bot.dao.repository.UserRepository
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import su.vshk.billing.bot.dao.model.EnabledNotificationUserDto
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
        userId: Long,
        login: String,
        agreementId: Long
    ): Mono<UserEntity> =
        Mono.fromCallable {
            UserEntity(
                telegramId = telegramId,
                userId = userId,
                login = login,
                agreementId = agreementId
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

    fun findUsersEnabledNotification(): Mono<List<EnabledNotificationUserDto>> =
        Mono.fromCallable {
            userRepository.findUsersEnabledNotification()
        }.subscribeOn(Schedulers.boundedElastic())
}