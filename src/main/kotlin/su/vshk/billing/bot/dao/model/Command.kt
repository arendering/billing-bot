package su.vshk.billing.bot.dao.model

import su.vshk.billing.bot.dialog.step.*

class Command private constructor(
    /**
     * Значение команды
     */
    val value: String,
    /**
     * Является ли команда диалоговой
     */
    val isDialog: Boolean = false,
    /**
     * Является ли команда служебной
     */
    val isService: Boolean = false,
    /**
     * Шаги для диалоговой команды
     */
    val steps: List<String>? = null
) {
    companion object {
        private val commands = mutableMapOf<String, Command>()
        private val add: (Command) -> Unit = { commands[it.value] = it }

        val LOGIN = Command(value = "/start", isDialog = true, steps = getLoginSteps())
        val MENU = Command(value = "/menu")
        val AGREEMENTS = Command(value = "/agreements", isDialog = true, steps = getAgreementSteps())
        val YOOKASSA_PAYMENT = Command(value = "/yookassa_payment", isDialog = true, steps = getYookassaPaymentSteps())
        val PAYMENT_HISTORY = Command(value = "/payment_history", isDialog = true, steps = getPaymentHistorySteps())
        val PROMISE_PAYMENT = Command(value = "/promise_payment", isDialog = true, steps = getPromisePaymentSteps())
        val TARIFFS = Command(value = "/tariffs")
        val NOTIFICATION = Command(value = "/notification", isDialog = true, steps = getNotificationSteps())
        val CONTACTS = Command(value = "/contacts")
        val EXIT = Command(value = "/exit", isDialog = true, steps = getExitSteps())
        val DELETE_PAYMENT_NOTIFICATION = Command(value = "/delete_payment_notification", isService = true)

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
            add(DELETE_PAYMENT_NOTIFICATION)
        }

        fun get(code: String): Command? =
            commands[code]

        private fun getLoginSteps() = listOf(LoginStep.LOGIN, LoginStep.PASSWORD)
        private fun getAgreementSteps() = listOf(AgreementStep.INFO, AgreementStep.SWITCH_AGREEMENT)
        private fun getYookassaPaymentSteps() = listOf(YookassaPaymentStep.AMOUNT)
        private fun getPaymentHistorySteps() = listOf(PaymentHistoryStep.PERIOD)
        private fun getPromisePaymentSteps() = listOf(PromisePaymentStep.WARNING, PromisePaymentStep.AMOUNT)
        private fun getNotificationSteps() = listOf(NotificationStep.SWITCH)
        private fun getExitSteps() = listOf(ExitStep.WARNING)
    }

    fun isStart(): Boolean =
        this == LOGIN
}