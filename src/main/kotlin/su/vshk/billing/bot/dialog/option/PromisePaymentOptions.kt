package su.vshk.billing.bot.dialog.option

class PromisePaymentAvailableOptions private constructor() {
    companion object {
        const val WARNING_APPROVE = "/approve"
        const val AMOUNT_PLUS_ONE = "+1"
        const val AMOUNT_MINUS_ONE = "-1"
        const val AMOUNT_PLUS_TWENTY_FIVE = "+25"
        const val AMOUNT_MINUS_TWENTY_FIVE = "-25"
        const val AMOUNT_PLUS_ONE_HUNDRED = "+100"
        const val AMOUNT_MINUS_ONE_HUNDRED = "-100"
        const val AMOUNT_SUBMIT = "/submit"

        val allAmountOptions = listOf(
            AMOUNT_PLUS_ONE,
            AMOUNT_MINUS_ONE,
            AMOUNT_PLUS_TWENTY_FIVE,
            AMOUNT_MINUS_TWENTY_FIVE,
            AMOUNT_PLUS_ONE_HUNDRED,
            AMOUNT_MINUS_ONE_HUNDRED,
            AMOUNT_SUBMIT
        )
    }
}

data class PromisePaymentOptions(
    var amount: Int? = null
)