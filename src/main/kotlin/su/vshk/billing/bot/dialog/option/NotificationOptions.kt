package su.vshk.billing.bot.dialog.option

class NotificationAvailableOptions private constructor() {
    companion object {
        /**
         * Включить оповещение для текущего договора
         */
        const val ENABLE_FOR_SINGLE_AGREEMENT = "enable_for_single_agreement"

        /**
         * Включить оповещение для всех договоров
         */
        const val ENABLE_FOR_ALL_AGREEMENTS = "enable_for_all_agreements"

        /**
         * Включить оповещения (если у пользователя только один договор).
         */
        const val ENABLE = "enable"

        /**
         * Отключить оповещение
         */
        const val DISABLE = "disable"
    }
}

data class NotificationOptions(
    val switch: String? = null
)
