package su.vshk.billing.bot.util

import su.vshk.billing.bot.web.client.BillingMethod

class AuthFailedException: RuntimeException()

class InternalException(private val msg: String)
    : RuntimeException("Internal error: $msg")

class BillingBadResponseException(private val msg: String)
    : RuntimeException("Billing bad response: $msg")

class GetVgroupsBadResponseException(private val msg: String)
    : RuntimeException("${BillingMethod.GET_VGROUPS} bad response: $msg")

class GetSbssKnowledgeBadResponseException(private val msg: String)
    : RuntimeException("${BillingMethod.GET_SBSS_KNOWLEDGE} bad response: $msg")

class GetAccountBadResponseException(private val msg: String)
    : RuntimeException("${BillingMethod.GET_ACCOUNT} bad response: $msg")

class InsertPrePaymentBadResponseException(private val msg: String)
    : RuntimeException("${BillingMethod.INSERT_PRE_PAYMENT} bad response: $msg")

class ClientLoginBadResponseException(private val msg: String)
    : RuntimeException("${BillingMethod.CLIENT_LOGIN} bad response: $msg")

class ManagerLoginBadResponseException(private val msg: String)
    : RuntimeException("${BillingMethod.MANAGER_LOGIN} bad response: $msg")

class GetRecommendedPaymentBadResponseException(private val msg: String)
    : RuntimeException("${BillingMethod.GET_RECOMMENDED_PAYMENT} bad response: $msg")

class YookassaBadResponseException(private val path: String, private val msg: String)
    : RuntimeException("Yookassa $path error: $msg")

class YookassaCreatePaymentBadResponseException(private val msg: String)
    : RuntimeException("Yookassa create payment error: $msg")

class CalculatorException(private val msg: String)
    : RuntimeException("Calculator error: $msg")

class UnexpectedDialogOptionException(private val option: String?, private val command: String, private val step: String)
    : RuntimeException("Unexpected option '$option' for command $command and step '$step'")
