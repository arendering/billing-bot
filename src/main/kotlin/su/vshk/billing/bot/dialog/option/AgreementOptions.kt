package su.vshk.billing.bot.dialog.option

import su.vshk.billing.bot.service.dto.AgreementDto

class AgreementAvailableOptions private constructor() {
    companion object {
        /**
         * Отмена диалога на шаге с общей информацией (актуально для пользователя с несколькими договорами)
         */
        const val CANCEL_INFO = "/cancel_info"

        /**
         * Сменить договор на шаге с общей информацией (актуально для пользователя с несколькими договорами)
         */
        const val SWITCH_AGREEMENT = "/switch_agreement"

        /**
         * Отмена диалога на шаге с выбором нового договора (актуально для пользователя с несколькими договорами)
         */
        const val CANCEL_SWITCH_AGREEMENT = "/cancel_switch_agreement"
    }
}

data class AgreementOptions(
    val agreement: AgreementDto? = null
)
