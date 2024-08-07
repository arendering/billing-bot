package su.vshk.billing.bot.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PhoneNormalizerTest {

    @Test
    fun testNormalizePhoneForYookassa() {
        assertThat(
            PhoneNormalizer.normalizeForYookassa(null)
        ).isNull()

        var phone = "+79991112233"

        assertThat(
            PhoneNormalizer.normalizeForYookassa(phone)
        ).isEqualTo("79991112233")

        phone = "89991112233"
        assertThat(
            PhoneNormalizer.normalizeForYookassa(phone)
        ).isEqualTo("89991112233")
    }
}