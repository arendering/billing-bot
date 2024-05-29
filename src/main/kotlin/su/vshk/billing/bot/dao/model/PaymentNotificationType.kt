package su.vshk.billing.bot.dao.model

class PaymentNotificationType private constructor() {
    companion object {
        /**
         * Отправка напоминания по текущему договору.
         */
        const val SINGLE = "SINGLE"

        /**
         * Отправка напоминания по всем доступным договорам.
         */
        const val ALL = "ALL"
    }
}