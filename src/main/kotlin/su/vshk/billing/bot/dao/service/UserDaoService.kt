package su.vshk.billing.bot.dao.service

import su.vshk.billing.bot.dao.model.UserEntity
import su.vshk.billing.bot.dao.repository.UserRepository
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import su.vshk.billing.bot.dao.model.EnabledNotificationUserDto
import su.vshk.billing.bot.util.getLogger
import java.util.*

@Service
class UserDaoService(
    private val userRepository: UserRepository
) {

    private val logger = getLogger()

    fun findUser(telegramId: Long): Mono<Optional<UserEntity>> =
        Mono.fromCallable {
            val user = userRepository.findById(telegramId)
            logger.debug("DAO findUser, found user ${user.orElse(null)}")
            user
        }.subscribeOn(Schedulers.boundedElastic())

    fun saveUser(
        telegramId: Long,
        userId: Long,
        login: String,
        agreementId: Long
    ): Mono<UserEntity> =
        Mono.fromCallable {
            val user = UserEntity(telegramId = telegramId, userId = userId, login = login, agreementId = agreementId)
            logger.debug("DAO saveUser, saved user $user")
            userRepository.save(user)
        }.subscribeOn(Schedulers.boundedElastic())

    fun updateUser(updated: UserEntity): Mono<UserEntity> =
        Mono.fromCallable {
            logger.debug("DAO updateUser, updated user $updated")
            userRepository.save(updated)
        }.subscribeOn(Schedulers.boundedElastic())

    fun deleteUser(telegramId: Long): Mono<Unit> =
        findUser(telegramId)
            .flatMap { userOpt ->
                val user = userOpt.get()
                logger.debug("DAO deleteUser, deleted user $user")
                doDeleteUser(user)
            }

    fun findUsersEnabledNotification(): Mono<List<EnabledNotificationUserDto>> =
        Mono.fromCallable {
            userRepository.findUsersEnabledNotification()
        }.subscribeOn(Schedulers.boundedElastic())

    private fun doDeleteUser(user: UserEntity): Mono<Unit> =
        Mono.fromCallable {
            userRepository.delete(user)
        }.subscribeOn(Schedulers.boundedElastic())
}