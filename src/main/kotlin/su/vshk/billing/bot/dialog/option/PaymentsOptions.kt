package su.vshk.billing.bot.dialog.option

class PaymentsAvailableOptions private constructor() {
    companion object {
        const val PERIOD_ONE_MONTH = "1"
        const val PERIOD_THREE_MONTHS = "3"
        const val PERIOD_SIX_MONTHS = "6"

        val availablePeriods = listOf(
            PERIOD_ONE_MONTH,
            PERIOD_THREE_MONTHS,
            PERIOD_SIX_MONTHS
        )
    }
}

data class PaymentsOptions(
    var period: String? = null
)

class PaymentsPeriod private constructor() {
    companion object {
        const val ONE_MONTH = "1"
        const val THREE_MONTHS = "3"
        const val SIX_MONTHS = "6"
    }
}