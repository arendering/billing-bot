package su.vshk.billing.bot.web.dto.manager

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import su.vshk.billing.bot.util.AmountDeserializer
import java.math.BigDecimal

data class GetRecommendedPaymentRequest(
    @JsonProperty("id")
    val agreementId: Long? = null,

    @JsonProperty("mode")
    val mode: Long? = null
)

data class GetRecommendedPaymentResponse(
    @JsonDeserialize(using = AmountDeserializer::class)
    @JsonProperty("ret")
    val amount: BigDecimal? = null
)
