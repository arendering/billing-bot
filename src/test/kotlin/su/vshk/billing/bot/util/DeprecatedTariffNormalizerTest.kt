package su.vshk.billing.bot.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import su.vshk.billing.bot.dao.model.TariffType

class DeprecatedTariffNormalizerTest {

    @ParameterizedTest
    @CsvSource(
        "ТВ - (Архив) Смотрешка 25 за 25,(Архив) Смотрешка 25 за 25",
        "ТВ - 24 часа ТВ (Базовый),24 часа ТВ (Базовый)"
    )
    fun testNormalizeTvTariffs(tariff: String, expected: String) {
        DeprecatedTariffNormalizer.normalize(tariff)
            .let { normalized ->
                assertThat(normalized!!.type).isEqualTo(TariffType.DEPRECATED_TV)
                assertThat(normalized.name).isEqualTo(expected)
            }
    }

    @ParameterizedTest
    @CsvSource(
        "Свои (SE),Свои",
        "ФЛ - Акция 3 мес (SE),Акция 3 мес",
        "ФЛ - Подключение (SE),Подключение",
        "ФЛ - МКД - Качок1 (MX),Качок1",
        "ФЛ - ЧД - Качок2 (MX),Качок2",
        "ЮЛ - Бюджетный (SE),Бюджетный"
    )
    fun testNormalizeInternetTariffs(tariff: String, expected: String) {
        DeprecatedTariffNormalizer.normalize(tariff)
            .let { normalized ->
                assertThat(normalized!!.type).isEqualTo(TariffType.DEPRECATED_INTERNET)
                assertThat(normalized.name).isEqualTo(expected)
            }
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
        "ТВ - 24 часа ТВ (Отключен)",
        "ТВ - Смотрешка Отключена",
        "Услуги",
        ]
    )
    fun testNormalizeDisabledTariffs(tariff: String) {
        assertThat(DeprecatedTariffNormalizer.normalize(tariff)).isNull()
    }

}