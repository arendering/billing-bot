package su.vshk.billing.bot.service

import su.vshk.billing.bot.web.client.BillingWebClient
import su.vshk.billing.bot.web.dto.manager.GetRecommendedPaymentRequest
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.math.BigDecimal

@Service
class RecommendedPaymentService(
    private val billingWebClient: BillingWebClient
) {

    companion object {
        /**
         * Рекомендованный платеж по умолчанию.
         */
        private const val DEFAULT_MODE = 0L

        /**
         * Рекомендованный платеж за вычетом баланса.
         */
        private const val ACTUAL_MODE = 1L
    }

    fun getDefault(agrmId: Long): Mono<BigDecimal> =
        doGetRecommendedPayment(agrmId = agrmId, mode = DEFAULT_MODE)

    fun getActual(agrmId: Long): Mono<BigDecimal> =
        doGetRecommendedPayment(agrmId = agrmId, mode = ACTUAL_MODE)

    private fun doGetRecommendedPayment(agrmId: Long, mode: Long): Mono<BigDecimal> =
        billingWebClient
            .getRecommendedPayment(
                GetRecommendedPaymentRequest(
                    agrmId = agrmId,
                    mode = mode
                )
            )
            .map {
                it.ret ?: throw RuntimeException("getRecommendedPayment payload is null")
            }
}