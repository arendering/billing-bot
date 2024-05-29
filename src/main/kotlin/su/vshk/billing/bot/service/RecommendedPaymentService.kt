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

    fun getDefault(agreementId: Long): Mono<BigDecimal> =
        doGetRecommendedPayment(agreementId = agreementId, mode = DEFAULT_MODE)

    fun getActual(agreementId: Long): Mono<BigDecimal> =
        doGetRecommendedPayment(agreementId = agreementId, mode = ACTUAL_MODE)

    private fun doGetRecommendedPayment(agreementId: Long, mode: Long): Mono<BigDecimal> =
        billingWebClient
            .getRecommendedPayment(
                GetRecommendedPaymentRequest(
                    agreementId = agreementId,
                    mode = mode
                )
            )
            .map {
                it.amount ?: throw RuntimeException("getRecommendedPayment payload is null")
            }
}