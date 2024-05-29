package su.vshk.billing.bot.service.dto

import java.math.BigDecimal

data class PaymentHistoryDto(
    val dateFrom: String,
    val dateTo: String,
    val payments: List<PaymentDto>? = null
) {
    data class PaymentDto(
        val date: String? = null,
        val time: String? = null,
        val id: String? = null,
        val amount: BigDecimal? = null,
        val manager: String? = null
    )
}
