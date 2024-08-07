package su.vshk.billing.bot.dao.model

class Command private constructor(
    /**
     * Значение команды
     */
    val value: String,
    /**
     * Является ли команда диалоговой
     */
    val isDialog: Boolean = false
) {
    companion object {
        private val commands = mutableMapOf<String, Command>()
        private val add: (Command) -> Unit = { commands[it.value] = it }

        val LOGIN = Command(value = "/start", isDialog = true)
        val MENU = Command(value = "/menu")
        val AGREEMENTS = Command(value = "/agreements", isDialog = true)
        val YOOKASSA_PAYMENT = Command(value = "/yookassa_payment", isDialog = true)
        val PAYMENT_HISTORY = Command(value = "/payment_history", isDialog = true)
        val PROMISE_PAYMENT = Command(value = "/promise_payment", isDialog = true)
        val TARIFFS = Command(value = "/tariffs")
        val NOTIFICATION = Command(value = "/notification", isDialog = true)
        val CONTACTS = Command(value = "/contacts")
        val EXIT = Command(value = "/exit", isDialog = true)

        init {
            add(LOGIN)
            add(MENU)
            add(AGREEMENTS)
            add(YOOKASSA_PAYMENT)
            add(PAYMENT_HISTORY)
            add(PROMISE_PAYMENT)
            add(TARIFFS)
            add(NOTIFICATION)
            add(CONTACTS)
            add(EXIT)
        }

        fun get(code: String): Command? =
            commands[code]
    }
}