package su.vshk.billing.bot.web.dto.client

import com.fasterxml.jackson.annotation.JsonProperty

data class ClientPromisePaymentRequest(
    @JsonProperty("agrm")
    val agrmId: Long? = null,
    @JsonProperty("summ")
    val amount: Int? = null
)

data class ClientPromisePaymentResponse(
    @JsonProperty("ret")
    val ret: Long? = null
)