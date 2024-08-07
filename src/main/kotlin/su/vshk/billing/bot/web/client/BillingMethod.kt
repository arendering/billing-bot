package su.vshk.billing.bot.web.client

class BillingMethod private constructor() {
    companion object {
        const val MANAGER_LOGIN = "Login"
        const val CLIENT_LOGIN = "ClientLogin"
        const val GET_VGROUPS = "getVgroups"
        const val GET_PAYMENTS = "getPayments"
        const val GET_ACCOUNT = "getAccount"
        const val CLIENT_PROMISE_PAYMENT = "ClientPromisePayment"
        const val GET_RECOMMENDED_PAYMENT = "getRecommendedPayment"
        const val GET_SBSS_KNOWLEDGE = "getSbssKnowledge"
        const val INSERT_PRE_PAYMENT = "insPrePayment"
    }
}