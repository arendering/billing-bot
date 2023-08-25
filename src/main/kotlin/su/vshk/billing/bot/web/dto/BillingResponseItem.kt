package su.vshk.billing.bot.web.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class BillingResponseItem<T>(
    val data: T? = null,
    val fault: Fault? = null
) {
    fun isFault(): Boolean = data == null && fault != null
}

data class Fault(
    @JsonProperty("faultcode")
    val faultCode: String? = null,

    @JsonProperty("faultstring")
    val faultString: String? = null,

    val detail: String? = null
)