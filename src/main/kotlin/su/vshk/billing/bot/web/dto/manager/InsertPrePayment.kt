package su.vshk.billing.bot.web.dto.manager

import com.fasterxml.jackson.annotation.JsonProperty

data class InsertPrePaymentRequest(
    @JsonProperty("val")
    val value: InsertPrePaymentRequestValue? = null
)

data class InsertPrePaymentRequestValue(
    @JsonProperty("agrmid")
    val agreementId: Long? = null,

    @JsonProperty("amount")
    val amount: Double? = null
)

data class InsertPrePaymentResponse(
    @JsonProperty("ret")
    val prePaymentId: Long? = null
)