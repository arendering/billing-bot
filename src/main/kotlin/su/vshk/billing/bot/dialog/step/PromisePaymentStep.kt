package su.vshk.billing.bot.dialog.step

class PromisePaymentStep {
    companion object {
        const val WARNING = "warning"
        const val AMOUNT = "amount"
    }
}

data class PromisePaymentAmountStepData(
    /**
     * Начальное значение суммы.
     */
    val recommendedAmount: Int? = null,
    /**
     * Сумма, которая выбирается через калькулятор.
     */
    val amount: Int? = null
)