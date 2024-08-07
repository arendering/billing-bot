package su.vshk.billing.bot.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "bot")
data class BotProperties(
    var token: String? = null,
    var name: String? = null,
    var webClient: WebClientProperties = WebClientProperties(),
    var errorGroupNotification: ErrorGroupNotificationProperties = ErrorGroupNotificationProperties(),
    var paymentNotification: PaymentNotificationProperties = PaymentNotificationProperties(),
    var cache: CacheProperties = CacheProperties(),
    var yookassaPayment: YookassaPaymentProperties = YookassaPaymentProperties()
) {

    data class WebClientProperties(
        var scheme: String = "http",
        var host: String? = null,
        var port: String? = null,
        var connTimeoutMillis: Int = 5_000,
        var readTimeoutMillis: Long = 5_000L,
        var writeTimeoutMillis: Long = 5_000L,
        var manager: ManagerCredentials = ManagerCredentials()
    )

    data class ErrorGroupNotificationProperties(
        var enabled: Boolean = false,
        var chatId: Long? = null
    )

    data class PaymentNotificationProperties(
        var billingRequestDelaySeconds: Long = 1L
    )

    data class ManagerCredentials(
        var login: String? = null,
        var password: String? = null
    )

    data class CacheProperties(
        var sbssKnowledgeExpiredHours: Long = 24L
    )

    data class YookassaPaymentProperties(
        var returnUrl: String? = null,
        var scheme: String? = "https",
        var host: String = "api.yookassa.ru",
        var port: String? = "443",
        var path: String? = "v3/payments",
        var shopId: String? = null,
        var secretKey: String? = null
    )
}
