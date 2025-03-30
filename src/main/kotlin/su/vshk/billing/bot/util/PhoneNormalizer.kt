package su.vshk.billing.bot.util

class PhoneNormalizer private constructor() {
    companion object {

        private val logger = getLogger()

        /**
         * Нормализует номер телефона из биллинга в формат Юкассы.
         */
        fun normalizeForYookassa(phone: String?): String? {
            logger.debug("Try to normalize phone '$phone'")

            val normalized =
                if (phone?.first() == '+') {
                    phone.drop(1)
                } else {
                    phone
                }

            logger.debug("Normalized phone '$normalized'")

            return normalized
        }
    }
}