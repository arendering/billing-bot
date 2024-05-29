package su.vshk.billing.bot.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.math.BigDecimal

class AmountUtilsTest {

    @ParameterizedTest
    @CsvSource(value = [
        "2500.1234,2 500.13",
        "-13450.1495,-13 450.15"
    ])
    fun testFormatAmount(amount: String, expected: String) {
        assertThat(
            AmountUtils.formatAmount(
                amount = BigDecimal(amount),
                splitByThousands = true,
                currencyChar = null
            )
        ).isEqualTo(expected)
    }

    @ParameterizedTest
    @CsvSource(value = [
        "100.25,101",
        "100.75,101",
        "-100.25,-101",
        "-100.75,-101",
        "0,0",
        "0.25,1",
        "0.65,1",
        "-0.65,-1"
    ])
    fun testIntegerRound(amount: String, expected: String) {
        assertThat(
            AmountUtils.integerRound(BigDecimal(amount))
        ).isEqualTo(
            expected.toInt()
        )
    }

    @ParameterizedTest
    @CsvSource(value = [
        "0.00,true",
        "-0.00,true",
        "0.01,false",
        "-0.01,false",
        "5.33,false",
        "-5.33,false"
    ])
    fun testIsZero(amount: String, expected: Boolean) {
        assertThat(
            AmountUtils.isZero(BigDecimal(amount))
        ).isEqualTo(
            expected
        )
    }
}
