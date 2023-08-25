package su.vshk.billing.bot.service

import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import su.vshk.billing.bot.dao.model.UserEntity
import su.vshk.billing.bot.dao.service.UserDaoService
import su.vshk.billing.bot.util.WebUtils
import su.vshk.billing.bot.web.client.BillingCredentialsWebClient
import su.vshk.billing.bot.web.client.BillingLoginWebClient
import su.vshk.billing.bot.web.dto.client.ClientLoginRequest
import su.vshk.billing.bot.web.dto.manager.GetAccountRequest
import java.util.*

@Service
class LoginService(
    private val loginWebClient: BillingLoginWebClient,
    private val credentialsWebClient: BillingCredentialsWebClient,
    private val userDaoService: UserDaoService,
    private val cookieManager: CookieManager
) {

    /**
     * Выполняет логин пользователя.
     *
     * @param request запрос
     * @return uid
     */
    fun clientLogin(request: ClientLoginRequest): Mono<Optional<Long>> =
        loginWebClient.clientLogin(request)
            .flatMap {
                it.orElse(null)
                    ?.let { clientCookie ->
                        cookieManager
                            .putClientCookie(uid = clientCookie.uid, cookie = clientCookie.cookie)
                            .map { Optional.of(clientCookie.uid) }
                    }
                    ?: Optional.empty<Long>().toMono()
            }

    /**
     * Получает актуальную клиентскую куку.
     *
     * @param user пользователь
     * @return актуальная клиентская кука
     */
    //TODO: при обновлении пароля у user он остается старый. Мб возвращать обновленного юзера ?
    fun getClientCookie(user: UserEntity): Mono<String> =
        cookieManager.getClientCookie(user.uid!!)
            .flatMap { cookieOpt ->
                if (cookieOpt.isPresent) {
                    cookieOpt.get().toMono()
                } else {
                    val request = ClientLoginRequest(login = user.login, password = user.password)
                    loginWebClient.getClientCookie(request)
                        .flatMap { firstAttemptCookie ->
                            if (firstAttemptCookie.isPresent) {
                                cookieManager.putClientCookie(uid = user.uid, cookie = firstAttemptCookie.get())
                            } else {
                                updatePassword(user)
                                    .flatMap { actualPassword -> loginWebClient.getClientCookie(request.copy(password = actualPassword)) }
                                    .flatMap { secondAttemptCookie ->
                                        if (secondAttemptCookie.isPresent) {
                                            cookieManager.putClientCookie(uid = user.uid, cookie = secondAttemptCookie.get())
                                        } else {
                                            throw RuntimeException("could not get client cookie with updated password")
                                        }
                                    }
                            }
                        }
                }
            }

    /**
     * Получает куку менеджера.
     *
     * @return кука менеджера
     */
    fun getManagerCookie(): Mono<String> =
        cookieManager.getManagerCookie()
            .flatMap { cookieOpt ->
                if (cookieOpt.isPresent) {
                    cookieOpt.get().toMono()
                } else {
                    loginWebClient.getManagerCookie()
                        .flatMap { cookieManager.putManagerCookie(it) }
                }
            }

    private fun updatePassword(user: UserEntity): Mono<String> =
        WebUtils.retryIfAuthFailedExecute {
            getManagerCookie()
                .flatMap {
                    val request = GetAccountRequest(uid = user.uid!!)
                    credentialsWebClient.getActualPassword(managerCookie = it, request = request)
                }
        }
            .map {
                it.data?.ret?.account?.password
                    ?: throw RuntimeException("actual password is null")
            }
            .flatMap { actualPassword ->
                user.copy(password = actualPassword)
                    .let { userDaoService.updateUser(it) }
            }
            .map { it.password }
}