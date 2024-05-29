package su.vshk.billing.bot.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*

class AmountUtils private constructor() {
    companion object {
        private val defaultFormatSymbols = object : DecimalFormatSymbols(Locale.ENGLISH) {
            override fun getGroupingSeparator(): Char = ' '
            override fun getDecimalSeparator(): Char = '.'
        }

        private val amountValueFormatter = ThreadLocal.withInitial {
            DecimalFormat("0.00", defaultFormatSymbols)
        }

        private val amountDisplayValueFormatter = ThreadLocal.withInitial {
            DecimalFormat("#,##0.00", defaultFormatSymbols)
        }

        fun formatAmount(
            amount: BigDecimal,
            splitByThousands: Boolean = true,
            currencyChar: Char? = '₽'
        ): String =
            //TODO: тут не нужно округлять, все BigDecimal в коде уже округлены
            amount.setScale(2, RoundingMode.UP)
                .let { rounded ->
                    if (splitByThousands) {
                        amountDisplayValueFormatter.get().format(rounded)
                    } else {
                        amountValueFormatter.get().format(rounded)
                    }.run {
                        this.replace("""\.00$""".toRegex(), "")
                    }
                }
                .let { formatted ->
                    currencyChar
                        ?.let { c -> "$formatted $c" }
                        ?: formatted
                }

        /**
         * Округляет до целого.
         *
         * @param amount сумма
         * @return округленная сумма
         */
        fun integerRound(amount: BigDecimal): Int =
            amount.setScale(0, RoundingMode.UP).toInt()

        fun isZero(amount: BigDecimal): Boolean =
            integerRound(amount) == 0
    }
}