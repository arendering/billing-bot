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

        val START = Command(value = "/start", isDialog = true)
        val MENU = Command(value = "/menu")
        val INFO = Command(value = "/info")
        val PAYMENTS = Command(value = "/payments", isDialog = true)
        val PROMISE_PAYMENT = Command(value = "/promise_payment", isDialog = true)
        val TARIFFS = Command(value = "/tariffs")
        val NOTIFICATION = Command(value = "/notification", isDialog = true)
        val CONTACTS = Command(value = "/contacts")
        val EXIT = Command(value = "/exit", isDialog = true)

        init {
            add(START)
            add(MENU)
            add(INFO)
            add(PAYMENTS)
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