package su.vshk.billing.bot.web.dto.manager

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import su.vshk.billing.bot.util.AmountDeserializer
import java.math.BigDecimal

data class GetRecommendedPaymentRequest(
    @JsonProperty("id")
    val agrmId: Long? = null,

    @JsonProperty("mode")
    val mode: Long? = null
)

data class GetRecommendedPaymentResponse(
    @JsonProperty("ret")
    @JsonDeserialize(using = AmountDeserializer::class)
    val ret: BigDecimal? = null
)
