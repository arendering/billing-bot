package su.vshk.billing.bot.util

import su.vshk.billing.bot.exception.AuthFailedException
import su.vshk.billing.bot.web.dto.RetryConfig
import su.vshk.billing.bot.web.dto.BillingResponseItem
import reactor.core.publisher.Mono
import reactor.util.retry.Retry
import java.time.Duration

class WebUtils private constructor() {
    companion object {
        private val log = getLogger()

        //TODO: если стратегия поиска куки в заголовках ответа работает, то этот костыль можно удалить
        /**
         * Это костыль, который нужен для случая, когда получив актуальную куку (клиента или менеджера) делается запрос
         * в биллинг, и в ответе возвращается error_auth. Просто пытаемся несколько раз выполнить целевой запрос.
         *
         * @param requestFunction целевой запрос в биллинг
         * @return ответ биллинга
         */
        fun <T> retryIfAuthFailedExecute(requestFunction: () -> Mono<BillingResponseItem<T>>): Mono<BillingResponseItem<T>> =
            Mono.defer {
                requestFunction.invoke()
                    .map {
                        if (it.fault?.faultString?.lowercase() == "error_auth") {
                            throw AuthFailedException()
                        } else {
                            it
                        }
                    }
            }.retryIfAuthFailed()

        private fun <T> Mono<T>.retryIfAuthFailed(
            retryConfig: RetryConfig = RetryConfig(5, 3000)
        ): Mono<T> =
            Mono.deferContextual { context ->
                this.retryWhen(
                    Retry
                        .fixedDelay(retryConfig.attempts, Duration.ofMillis(retryConfig.waitTimeMillis))
                        .doBeforeRetry {
                            log.errorTraceId(context, "error_auth occurs, attempt to re-send ${it.totalRetries() + 1}")
                        }
                        .filter { e -> e is AuthFailedException }
                )
            }
    }
}
