package su.vshk.billing.bot.util

class PhoneNormalizer private constructor() {
    companion object {

        private val logger = getLogger()

        /**
         * Нормализует номер телефона из биллинга в формат Юкассы.
         */
        fun normalizeForYookassa(phone: String?): String? {
            logger.debug("try to normalize phone '$phone'")

            val normalized =
                if (phone?.first() == '+') {
                    phone.drop(1)
                } else {
                    phone
                }

            logger.debug("normalized phone '$normalized'")

            return normalized
        }
    }
}