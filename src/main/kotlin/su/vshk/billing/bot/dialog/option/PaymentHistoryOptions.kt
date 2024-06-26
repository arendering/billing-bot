package su.vshk.billing.bot.dialog.option

class PaymentHistoryAvailableOptions private constructor() {
    companion object {
        const val PERIOD_ONE_MONTH = "1"
        const val PERIOD_THREE_MONTHS = "3"
        const val PERIOD_SIX_MONTHS = "6"
    }
}

data class PaymentHistoryOptions(
    val period: String? = null
)

class PaymentHistoryPeriod private constructor() {
    companion object {
        const val ONE_MONTH = "1"
        const val THREE_MONTHS = "3"
        const val SIX_MONTHS = "6"
    }
}