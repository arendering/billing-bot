package su.vshk.billing.bot.web.dto.manager

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import su.vshk.billing.bot.util.AmountDeserializer
import java.math.BigDecimal

data class GetVgroupsRequest(
    val flt: GetVgroupsFlt? = null
)

data class GetVgroupsFlt(
    @JsonProperty(value = "userid")
    val userId: Long? = null
)

data class GetVgroupsResponse(
    val ret: List<GetVgroupsRet>? = null
)

data class GetVgroupsRet(
    /**
     * ФИО
     */
    @JsonProperty("username")
    val username: String? = null,

    /**
     * Идентификатор договора
     */
    @JsonProperty("agrmid")
    val agrmId: Long? = null,

    /**
     * Номер договора
     */
    @JsonProperty("agrmnum")
    val agrmNum: String? = null,

    /**
     * Баланс
     */
    @JsonProperty("balance")
    @JsonDeserialize(using = AmountDeserializer::class)
    val balance: BigDecimal? = null,

    /**
     * Описание тарифа
     */
    @JsonProperty("tarifdescr")
    val tariffDescription: String? = null
)
