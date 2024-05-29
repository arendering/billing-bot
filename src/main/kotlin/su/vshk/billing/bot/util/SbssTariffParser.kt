package su.vshk.billing.bot.util

import su.vshk.billing.bot.service.dto.Tariff

class SbssTariffParser private constructor() {
    companion object {

        private val logger = getLogger()

        private val channelsRegex = """^\d+""".toRegex()

        /**
         * Парсит строку с информацией по тарифу, полученную из базы знаний.
         */
        fun parse(raw: String): Tariff? =
            try {
                raw
                    .split(',')
                    .map { token ->
                        val (key, value) = token.split(':', limit = 2)
                        Pair(key.trim(), value.trim())
                    }
                    .associate { it.first to it.second }
                    .let { values ->
                        Tariff(
                            id = values["tarid"]?.toLong() ?: throw RuntimeException("key 'tarid' not found in raw tariff text'$raw'"),
                            type = values["type"],
                            name = values["name"],
                            speed = values["speed"],
                            channels = values["channels"]?.let { channelsRegex.find(it) }?.value,
                            rent = values["rent"]
                        )
                    }
            } catch (th: Throwable) {
                logger.error("unable to parse tariff: '$raw'", th)
                null
            }
    }
}