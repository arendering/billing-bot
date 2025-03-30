package su.vshk.billing.bot.dialog.option

import su.vshk.billing.bot.web.dto.yookassa.YookassaPaymentReceiptCustomer

data class YookassaPaymentOptions(
    val customer: YookassaPaymentReceiptCustomer? = null,
    val amount: Int? = null
)