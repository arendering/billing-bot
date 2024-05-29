package su.vshk.billing.bot.web.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class BillingResponseItem<T>(
    val data: T? = null,
    val fault: Fault? = null
) {
    fun isFault(): Boolean = data == null && fault != null
}

data class Fault(
    @JsonProperty(value = "faultcode")
    val faultCode: String? = null,

    @JsonProperty(value = "faultstring")
    val faultString: String? = null,

    @JsonProperty(value = "detail")
    val detail: String? = null
)