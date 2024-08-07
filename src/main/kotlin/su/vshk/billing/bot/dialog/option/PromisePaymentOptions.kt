package su.vshk.billing.bot.dialog.option

class PromisePaymentAvailableOptions private constructor() {
    companion object {
        const val WARNING_APPROVE = "/approve"
    }
}

data class PromisePaymentOptions(
    val amount: Int? = null
)