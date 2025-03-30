package su.vshk.billing.bot.dao.model

class CalculatorButton private constructor() {
    companion object {
        const val ZERO = "0"
        const val ONE = "1"
        const val TWO = "2"
        const val THREE = "3"
        const val FOUR = "4"
        const val FIVE = "5"
        const val SIX = "6"
        const val SEVEN = "7"
        const val EIGHT = "8"
        const val NINE = "9"

        /**
         * Стереть последнюю цифру.
         */
        const val ERASE = "erase"

        /**
         * Очистить поле ввода
         */
        const val CLEAR = "clear"

        /**
         * Отправить сумму для пополнения баланса.
         */
        const val ENTER = "enter"

        /**
         * Отменить ввод суммы.
         */
        const val CANCEL_AMOUNT_STEP = "cancel_amount_step"

        val DIGITS = listOf(
            ZERO, ONE, TWO, THREE, FOUR, FIVE, SIX, SEVEN, EIGHT, NINE
        )
    }
}