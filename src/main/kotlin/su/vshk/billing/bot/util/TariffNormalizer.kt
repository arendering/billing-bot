package su.vshk.billing.bot.util

import su.vshk.billing.bot.service.dto.TariffsDto

class TariffNormalizer private constructor() {
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
         * Приводит к нормализованному виду тарифы.
         */
        fun normalizeTariffs(tariffs: List<String>): TariffsDto {
            val tvTariffs = mutableSetOf<String>()
            val internetTariffs = mutableSetOf<String>()

            tariffs
                .filterNot {
                    it.isEmpty()
                        || it.lowercase() == "услуги"
                        || it.lowercase().contains("отключ")
                }
                .forEach { tariff ->
                    if (tariff.startsWith(TV_PREFIX)) {
                        tvTariffs.add(
                            tariff.removePrefix(TV_PREFIX)
                        )
                    } else {
                        internetTariffs.add(
                            normalizeInternetTariff(tariff)
                        )
                    }
                }

            return TariffsDto(onlineTv = tvTariffs.toList(), internet = internetTariffs.toList())
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