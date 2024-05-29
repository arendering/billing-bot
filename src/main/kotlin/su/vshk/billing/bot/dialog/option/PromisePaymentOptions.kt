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
        const val CANCEL_AMOUNT_STEP = "/cancel_amount_step"

        val calculatorOptions = listOf(
            AMOUNT_PLUS_ONE,
            AMOUNT_MINUS_ONE,
            AMOUNT_PLUS_TWENTY_FIVE,
            AMOUNT_MINUS_TWENTY_FIVE,
            AMOUNT_PLUS_ONE_HUNDRED,
            AMOUNT_MINUS_ONE_HUNDRED
        )
    }
}

data class PromisePaymentOptions(
    val amount: Int? = null
)