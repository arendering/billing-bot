package su.vshk.billing.bot.dao.model

class TariffType private constructor() {
    companion object {
        /**
         * Тип тарифа для интернета из базы знаний
         */
        const val INTERNET = "internet"

        /**
         * Тип архивного тарифа для интернета
         */
        const val DEPRECATED_INTERNET = "deprecated-internet"

        /**
         * Тип тарифа для онлайн-тв из базы знаний
         */
        const val TV = "tv"

        /**
         * Тип арфивного тарифа для онлайн-тв
         */
        const val DEPRECATED_TV = "deprecated-tv"

        /**
         * Тип комбо тарифа из базы знаний
         */
        const val COMBO = "internet-tv"

        val INTERNET_ALL_TYPES = listOf(INTERNET, DEPRECATED_INTERNET)

        val TV_ALL_TYPES = listOf(TV, DEPRECATED_TV)

        val DEPRECATED_TYPES = listOf(DEPRECATED_INTERNET, DEPRECATED_TV)
    }
}