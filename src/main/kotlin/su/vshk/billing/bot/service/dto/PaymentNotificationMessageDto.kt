package su.vshk.billing.bot.service.dto

import java.math.BigDecimal

data class PaymentNotificationMessageDto(
    val address: String,
    val balance: BigDecimal,
    val actualRecommendedPayment: BigDecimal
)
