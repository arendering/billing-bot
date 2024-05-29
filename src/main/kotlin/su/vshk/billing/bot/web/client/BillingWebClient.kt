package su.vshk.billing.bot.web.client

import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import su.vshk.billing.bot.config.BotProperties
import su.vshk.billing.bot.dao.model.UserEntity
import su.vshk.billing.bot.service.BillingLoginService
import su.vshk.billing.bot.util.WebUtils
import su.vshk.billing.bot.web.converter.RequestConverter
import su.vshk.billing.bot.web.converter.ResponseConverter
import su.vshk.billing.bot.web.dto.BillingBaseResponse
import su.vshk.billing.bot.web.dto.BillingResponseItem
import su.vshk.billing.bot.web.dto.client.ClientLoginRequest
import su.vshk.billing.bot.web.dto.client.ClientPromisePaymentRequest
import su.vshk.billing.bot.web.dto.client.ClientPromisePaymentResponse
import su.vshk.billing.bot.web.dto.manager.*
import java.util.*

@Service
class BillingWebClient(
    webClient: WebClient,
    properties: BotProperties,
    private val requestConverter: RequestConverter,
    private val responseConverter: ResponseConverter,
    private val billingLoginService: BillingLoginService
): BillingBaseWebClient(webClient, properties) {

    /**
     * Получает userId пользователя.
     */
    fun getClientId(request: ClientLoginRequest): Mono<Optional<Long>> =
        billingLoginService.clientLogin(request)

    /**
     * Возвращает список учетных записей.
     */
    fun getVgroups(request: GetVgroupsRequest): Mono<GetVgroupsResponse> =
        WebUtils.retryIfAuthFailedExecute {
            billingLoginService.getManagerCookie()
                .flatMap {
                    doRequest(
                        method = BillingMethod.GET_VGROUPS,
                        cookie = it,
                        request = request,
                        responseClazz = GetVgroupsResponse::class.java
                    )
                }
        }.map { unwrapData(it) }

    /**
     * Возвращает список платежей.
     */
    fun getPayments(request: GetPaymentsRequest): Mono<GetPaymentsResponse> =
        WebUtils.retryIfAuthFailedExecute {
            billingLoginService.getManagerCookie()
                .flatMap {
                    doRequest(
                        method = BillingMethod.GET_PAYMENTS,
                        cookie = it,
                        request = request,
                        responseClazz = GetPaymentsResponse::class.java
                    )
                }
        }.map { unwrapData(it) }

    /**
     * Проводит обещанный платеж.
     */
    fun clientPromisePayment(user: UserEntity, request: ClientPromisePaymentRequest): Mono<BillingResponseItem<ClientPromisePaymentResponse>> =
        WebUtils.retryIfAuthFailedExecute {
            billingLoginService.getClientCookie(user)
                .flatMap {
                    doRequest(
                        method = BillingMethod.CLIENT_PROMISE_PAYMENT,
                        cookie = it,
                        request = request,
                        responseClazz = ClientPromisePaymentResponse::class.java
                    )
                }
        }

    /**
     * Получает рекомендованный платеж.
     */
    fun getRecommendedPayment(request: GetRecommendedPaymentRequest): Mono<GetRecommendedPaymentResponse> =
        WebUtils.retryIfAuthFailedExecute {
            billingLoginService.getManagerCookie()
                .flatMap {
                    doRequest(
                        method = BillingMethod.GET_RECOMMENDED_PAYMENT,
                        cookie = it,
                        request = request,
                        responseClazz = GetRecommendedPaymentResponse::class.java
                    )
                }
        }.map { unwrapData(it) }

    /**
     * Получает более подробную инфу о пользователе.
     */
    fun getAccount(request: GetAccountRequest): Mono<GetAccountResponse> =
        WebUtils.retryIfAuthFailedExecute {
            billingLoginService.getManagerCookie()
                .flatMap {
                    doRequest(
                        method = BillingMethod.GET_ACCOUNT,
                        cookie = it,
                        request = request,
                        responseClazz = GetAccountResponse::class.java
                    )
                }
        }.map { unwrapData(it) }

    /**
     * Получает инфу из базы знаний.
     */
    fun getSbssKnowledge(request: GetSbssKnowledgeRequest): Mono<GetSbssKnowledgeResponse> =
        WebUtils.retryIfAuthFailedExecute {
            billingLoginService.getManagerCookie()
                .flatMap {
                    doRequest(
                        method = BillingMethod.GET_SBSS_KNOWLEDGE,
                        cookie = it,
                        request = request,
                        responseClazz = GetSbssKnowledgeResponse::class.java
                    )
                }
        }.map { unwrapData(it) }

    private fun <Req, Resp> doRequest(
        method: String,
        cookie: String,
        request: Req,
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

    private fun <T> unwrapData(responseItem: BillingResponseItem<T>): T =
        if (responseItem.isFault()) {
            throw RuntimeException("get fault response")
        } else {
            responseItem.data ?: throw RuntimeException("response data is null")
        }
}