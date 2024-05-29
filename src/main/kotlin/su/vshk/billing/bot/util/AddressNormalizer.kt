package su.vshk.billing.bot.util

class AddressNormalizer private constructor() {

    companion object {
        private val logger = getLogger()
        private val commasRegex = """,+""".toRegex()
        private const val COMMA = ","
        private const val WHITESPACE = " "

        /**
         * Нормализация адреса для отображения в команде "Мои договоры"
         */
        fun agreementNormalize(address: String): String =
            normalize(address = address, dropFirst = 2)

        /**
         * Нормализация адреса для отображения в оповещении
         */
        fun notificationNormalize(address: String): String =
            normalize(address = address, dropFirst = 3)

        private fun normalize(address: String, dropFirst: Int): String =
            address
                .replace(commasRegex, COMMA)
                .split(COMMA)
                // для оповещений - отбрасываем страну, область и район
                // для договоров - отбрасываем страну и область
                .drop(dropFirst)
                // отбрасываем индекс
                .dropLast(1)
                .joinToString(separator = COMMA + WHITESPACE)
                .ifEmpty {
                    logger.warn("unable to normalize address '$address'")
                    address
                }

    }
}