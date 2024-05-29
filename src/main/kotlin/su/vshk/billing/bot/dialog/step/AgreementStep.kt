package su.vshk.billing.bot.dialog.step

class AgreementStep private constructor() {
    companion object {
        /**
         * Шаг с основной информацией по договору.
         */
        const val INFO = "info"

        /**
         * Шаг, на котором показываются доступные договоры.
         */
        const val SWITCH_AGREEMENT = "switch-agreement"
    }
}
