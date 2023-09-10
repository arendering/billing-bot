package su.vshk.billing.bot.dialog.step

class PromisePaymentStep {
    companion object {
        const val WARNING = "warning"
        const val AMOUNT = "amount"
    }
}

data class PromisePaymentAmountStepData(
    val amount: Int? = null
)