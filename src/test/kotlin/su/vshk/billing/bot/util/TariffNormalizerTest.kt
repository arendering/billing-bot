package su.vshk.billing.bot.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import su.vshk.billing.bot.util.TariffNormalizer

class TariffNormalizerTest {

    @Test
    fun testNormalizeTariffs() {
        val tariffs = listOf(
            // тв
            "ТВ - (Архив) Смотрешка 25 за 25",
            "ТВ - 24 часа ТВ (Базовый)",
            // интернет
            "Свои (SE)",
            "ФЛ - Акция 3 мес (SE)",
            "ФЛ - Подключение (SE)",
            "ФЛ - МКД - Качок1 (MX)",
            "ФЛ - ЧД - Качок2 (MX)",
            "ЮЛ - Бюджетный (SE)",
            // не должно попасть в итог
            "ТВ - 24 часа ТВ (Отключен)",
            "ТВ - Смотрешка Отключена",
            "Услуги",
            "Свои (SE)" // дубль, не должен попасть
        )

        val dto = TariffNormalizer.normalizeTariffs(tariffs)

        val tvTariffs = dto.onlineTv!!
        assertThat(tvTariffs.size).isEqualTo(2)
        assertThat(tvTariffs)
            .contains(
                "(Архив) Смотрешка 25 за 25",
                "24 часа ТВ (Базовый)"
            )

        val internetTariffs = dto.internet!!
        assertThat(internetTariffs.size).isEqualTo(6)
        assertThat(internetTariffs).contains(
            "Свои",
            "Акция 3 мес",
            "Подключение",
            "Качок1",
            "Качок2",
            "Бюджетный",
        )
    }
}