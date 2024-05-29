package su.vshk.billing.bot.web.client

import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import su.vshk.billing.bot.config.BotProperties
import su.vshk.billing.bot.web.converter.RequestConverter
import su.vshk.billing.bot.web.converter.ResponseConverter
import su.vshk.billing.bot.web.dto.BillingBaseResponse
import su.vshk.billing.bot.web.dto.BillingResponseItem
import su.vshk.billing.bot.web.dto.Cookie
import su.vshk.billing.bot.web.dto.Fault
import su.vshk.billing.bot.web.dto.client.ClientLoginCookie
import su.vshk.billing.bot.web.dto.client.ClientLoginRequest
import su.vshk.billing.bot.web.dto.client.ClientLoginResponse
import su.vshk.billing.bot.web.dto.manager.LoginRequest
import su.vshk.billing.bot.web.dto.manager.LoginResponse
import java.time.Instant
import java.util.*

@Service
class BillingLoginWebClient(
    webClient: WebClient,
    properties: BotProperties,
    private val requestConverter: RequestConverter,
    private val responseConverter: ResponseConverter
): BillingBaseWebClient(webClient, properties) {

    companion object {
        /**
         * Дельта, для подсчета времени протухания куки
         */
        private const val EXP_DELTA_SECONDS = 10L
    }

    /**
     * Выполняет логин для клиента.
     */
    fun clientLogin(request: ClientLoginRequest): Mono<Optional<ClientLoginCookie>> =
        doGetCookie(method = BillingMethod.CLIENT_LOGIN, request = request, responseClazz = ClientLoginResponse::class.java)
            .map { (cookie, cookieExpTimestamp, responseItem) ->
                responseItem.data?.ret?.userId
                    ?.let {
                        Optional.of(
                            ClientLoginCookie(
                                userId = it,
                                cookie = Cookie(
                                    value = cookie,
                                    expTimestampSeconds = cookieExpTimestamp
                                )
                            )
                        )
                    }
                    ?: Optional.empty()
            }

    /**
     * Получает куку клиента.
     */
    fun getClientCookie(request: ClientLoginRequest): Mono<Optional<Cookie>> =
        doGetCookie(method = BillingMethod.CLIENT_LOGIN, request = request, responseClazz = ClientLoginResponse::class.java)
            .map { (cookie, cookieExpTimestamp, responseItem) ->
                val fault = responseItem.fault

                when {
                    isInvalidCredentials(fault) ->
                        Optional.empty()

                    fault != null ->
                        throw RuntimeException("could not get client cookie: ${fault.faultString}")

                    else ->
                        Optional.of(
                            Cookie(value = cookie, expTimestampSeconds = cookieExpTimestamp)
                        )
                }
            }

    /**
     * Получает куку менеджера.
     */
    fun getManagerCookie(): Mono<Cookie> =
        Mono
            .defer {
                val request = LoginRequest(login = properties.webClient.manager.login, password = properties.webClient.manager.password)
                doGetCookie(method = BillingMethod.MANAGER_LOGIN, request = request, responseClazz = LoginResponse::class.java)
            }
            .map { (cookie, cookieExpTimestamp, responseItem) ->
                val fault = responseItem.fault

                if (fault == null) {
                    Cookie(value = cookie, expTimestampSeconds = cookieExpTimestamp)
                } else {
                    throw RuntimeException("could not get manager cookie: ${fault.faultString}")
                }
            }

    private fun <Req, Resp> doGetCookie(
        method: String,
        request: Req,
        responseClazz: Class<Resp>
    ): Mono<Triple<String, Long, BillingResponseItem<Resp>>> =
        Mono
            .fromCallable { requestConverter.convert(method = method, payload = request) }
            .flatMap { requestBody ->
                val now = Instant.now()
                doRequest(body = requestBody)
                    .map { response ->
                        val cookie = response.cookie!!.value
                        val expTimestamp = resolveCookieExpTimestamp(from = now, cookie = response.cookie)
                        val responseItem = toResponseItem(method = method, responseData = response, responseClazz = responseClazz)

                        Triple(cookie, expTimestamp, responseItem)
                    }
            }

    private fun resolveCookieExpTimestamp(from: Instant, cookie: ResponseCookie): Long =
        from.epochSecond + cookie.maxAge.seconds - EXP_DELTA_SECONDS

    //TODO: подумай, куда вынести
    private fun <T> toResponseItem(
        method: String,
        responseData: BillingBaseResponse,
        responseClazz: Class<T>
    ): BillingResponseItem<T> {
        val httpBody = responseData.body
            ?: throw RuntimeException("http body is null")

        return if (responseData.status?.is2xxSuccessful == true) {
            BillingResponseItem(
                data = responseConverter.convert(method = method, httpBody = httpBody, clazz = responseClazz)
            )
        } else {
            BillingResponseItem(fault = responseConverter.convertFault(httpBody))
        }
    }

    private fun isInvalidCredentials(fault: Fault?): Boolean =
        fault?.faultString?.lowercase()?.contains("invalid login/pass") == true
}