package su.vshk.billing.bot.web.client

import su.vshk.billing.bot.web.converter.RequestConverter
import su.vshk.billing.bot.web.converter.ResponseConverter
import su.vshk.billing.bot.web.dto.BillingBaseResponse
import su.vshk.billing.bot.web.dto.BillingResponseItem
import su.vshk.billing.bot.web.dto.manager.GetAccountRequest
import su.vshk.billing.bot.web.dto.manager.GetAccountResponse
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import su.vshk.billing.bot.config.BotProperties

@Service
class BillingCredentialsWebClient(
    webClient: WebClient,
    properties: BotProperties,
    private val requestConverter: RequestConverter,
    private val responseConverter: ResponseConverter,
): BillingBaseWebClient(webClient, properties) {

    /**
     * Получает актуальный пароль пользователя.
     */
    fun getActualPassword(managerCookie: String, request: GetAccountRequest): Mono<BillingResponseItem<GetAccountResponse>> =
        doRequest(
            method = BillingMethod.GET_ACCOUNT,
            request = request,
            cookie = managerCookie,
            responseClazz = GetAccountResponse::class.java
        )

    private fun <Req, Resp> doRequest(
        method: String,
        request: Req,
        cookie: String,
        responseClazz: Class<Resp>
    ): Mono<BillingResponseItem<Resp>> =
        Mono.fromCallable { requestConverter.convert(method = method, payload = request) }
            .flatMap { doRequest(body = it, cookie = cookie) }
            .map { toResponseItem(method = method, responseData = it, responseClazz = responseClazz) }

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
}