package su.vshk.billing.bot.service.dto

data class PaymentsDto(
    val dateFrom: String,
    val dateTo: String,
    val payments: List<PaymentDto>? = null
) {
    data class PaymentDto(
        val date: String? = null,
        val time: String? = null,
        val id: String? = null,
        val amount: String? = null,
        val manager: String? = null
    )
}
