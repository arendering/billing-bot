package su.vshk.billing.bot.util

import su.vshk.billing.bot.dao.model.TariffType
import su.vshk.billing.bot.service.dto.Tariff

class DeprecatedTariffNormalizer private constructor() {

    companion object {
        private const val TV_PREFIX = "ТВ - "

        private val INTERNET_PREFIXES = listOf(
            "ФЛ - МКД - ",
            "ФЛ - ЧД - ",
            "ФЛ - ",
            "ЮЛ - "
        )

        private val INTERNET_SUFFIXES = listOf(
            " (SE)",
            " (MX)"
        )

        /**
         * Нормализует архивный тариф, для которого не нашлось описания в базе знаний.
         */
        fun normalize(tariff: String): Tariff? =
            when {
                tariff.isEmpty() || tariff.lowercase() == "услуги" || tariff.lowercase().contains("отключ") ->
                    null

                tariff.startsWith(TV_PREFIX) ->
                    Tariff(type = TariffType.DEPRECATED_TV, name = tariff.removePrefix(TV_PREFIX))

                else ->
                    Tariff(type = TariffType.DEPRECATED_INTERNET, name = normalizeInternetTariff(tariff))
            }

        private fun normalizeInternetTariff(tariff: String): String {
            var normalized = tariff

            INTERNET_PREFIXES.forEach { prefix ->
                if (normalized.startsWith(prefix)) {
                    normalized = normalized.removePrefix(prefix)
                    return@forEach
                }
            }

            INTERNET_SUFFIXES.forEach { suffix ->
                if (normalized.endsWith(suffix)) {
                    normalized = normalized.removeSuffix(suffix)
                    return@forEach
                }
            }

            return normalized
        }
    }
}