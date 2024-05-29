package su.vshk.billing.bot.dao.model

class BlockedStatus private constructor() {
    companion object {
        /**
         * УЗ активна
         */
        const val ACTIVE = 0L

        /**
         * УЗ заблокирована по балансу
         */
        val BALANCE = listOf(1L, 4L)

        /**
         * УЗ заблокирована пользователем
         */
        const val USER_BLOCK = 2L

        /**
         * УЗ заблокирована администратором
         */
        const val ADMIN_BLOCK = 3L

        /**
         * УЗ заблокирована по лимиту трафика
         */
        const val TRAFFIC_LIMIT = 5L

        /**
         * УЗ отключена
         */
        const val DISABLED = 10L
    }
}