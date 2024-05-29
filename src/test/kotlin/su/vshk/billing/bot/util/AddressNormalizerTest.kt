package su.vshk.billing.bot.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AddressNormalizerTest {

    @Test
    fun testAgreementNormalize() {
        val address = "Россия,обл Московская,р-н Щелковский,,,кв-л Лесной,дом 8,,,,141181"
        assertThat(
            AddressNormalizer.agreementNormalize(address)
        ).isEqualTo(
            "р-н Щелковский, кв-л Лесной, дом 8"
        )
    }

    @Test
    fun testNotificationNormalize() {
        val address = "Россия,обл Московская,р-н Щелковский,,,кв-л Лесной,дом 8,,,,141181"
        assertThat(
            AddressNormalizer.notificationNormalize(address)
        ).isEqualTo(
            "кв-л Лесной, дом 8"
        )
    }
}