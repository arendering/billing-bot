package su.vshk.billing.bot.web.dto.manager

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import su.vshk.billing.bot.util.AmountDeserializer
import java.math.BigDecimal

data class GetVgroupsRequest(
    @JsonProperty(value = "flt")
    val filter: GetVgroupsFilter? = null
)

data class GetVgroupsFilter(
    @JsonProperty(value = "userid")
    val userId: Long? = null
)

data class GetVgroupsResponse(
    @JsonProperty(value = "ret")
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
    val agreementId: Long? = null,

    /**
     * Номер договора
     */
    @JsonProperty("agrmnum")
    val agreementNumber: String? = null,

    /**
     * Баланс
     */
    @JsonProperty("balance")
    @JsonDeserialize(using = AmountDeserializer::class)
    val balance: BigDecimal? = null,

    /**
     * Идентификатор тарифа
     */
    @JsonProperty("tarid")
    val tariffId: Long? = null,

    /**
     * Описание тарифа
     */
    @JsonProperty("tarifdescr")
    val tariffDescription: String? = null,

    /**
     * Адреса
     */
    @JsonProperty("address")
    val addresses: List<GetVgroupsAddress>? = null,

    /**
     * Описание агента
     */
    @JsonProperty("agentdescr")
    val agentDescription: String? = null,

    /**
     * Текущее состояние блокировки
     */
    @JsonProperty("blocked")
    val blocked: Long? = null
)

data class GetVgroupsAddress(
    @JsonProperty(value = "address")
    val address: String? = null
)
