package su.vshk.billing.bot.service

import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import su.vshk.billing.bot.dao.model.UserEntity
import su.vshk.billing.bot.util.WebUtils
import su.vshk.billing.bot.web.client.BillingCredentialsWebClient
import su.vshk.billing.bot.web.client.BillingLoginWebClient
import su.vshk.billing.bot.web.dto.client.ClientLoginRequest
import su.vshk.billing.bot.web.dto.manager.GetAccountRequest
import java.util.*

@Service
class BillingLoginService(
    private val loginWebClient: BillingLoginWebClient,
    private val credentialsWebClient: BillingCredentialsWebClient,
    private val cookieManager: CookieManager
) {

    /**
     * Выполняет логин пользователя.
     *
     * @param request запрос
     * @return userId
     */
    fun clientLogin(request: ClientLoginRequest): Mono<Optional<Long>> =
        loginWebClient.clientLogin(request)
            .flatMap {
                it.orElse(null)
                    ?.let { clientCookie ->
                        cookieManager
                            .putClientCookie(userId = clientCookie.userId, cookie = clientCookie.cookie)
                            .map { Optional.of(clientCookie.userId) }
                    }
                    ?: Optional.empty<Long>().toMono()
            }

    /**
     * Получает актуальную клиентскую куку.
     *
     * @param user пользователь
     * @return актуальная клиентская кука
     */
    fun getClientCookie(user: UserEntity): Mono<String> =
        cookieManager.getClientCookie(user.userId!!)
            .flatMap { cookieOpt ->
                if (cookieOpt.isPresent) {
                    cookieOpt.get().toMono()
                } else {
                    getActualPassword(user.userId)
                        .flatMap { password ->
                            val request = ClientLoginRequest(login = user.login, password = password)
                            loginWebClient.getClientCookie(request)
                        }
                        .flatMap { cookie ->
                            cookieManager.putClientCookie(userId = user.userId, cookie = cookie.get())
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

    private fun getActualPassword(userId: Long): Mono<String> =
        WebUtils
            .retryIfAuthFailedExecute {
                getManagerCookie()
                    .flatMap {
                        val request = GetAccountRequest(userId = userId)
                        credentialsWebClient.getActualPassword(managerCookie = it, request = request)
                    }
            }
            .map {
                it.data?.ret?.account?.password
                    ?: throw RuntimeException("actual password is null")
            }
}