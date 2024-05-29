package su.vshk.billing.bot.web.dto.manager

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import su.vshk.billing.bot.util.AmountDeserializer
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class GetPaymentsRequest(
    @JsonProperty("flt")
    val filter: GetPaymentsFilter? = null
)

data class GetPaymentsFilter(
    /**
     * Идентификатор договора
     */
    @JsonProperty("agrmid")
    val agreementId: Long? = null,

    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonProperty("dtfrom")
    val dateFrom: LocalDate? = null,

    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonProperty("dtto")
    val dateTo: LocalDate? = null
)

data class GetPaymentsResponse(
    @JsonProperty("ret")
    val ret: List<GetPaymentsRet>? = null
)

data class GetPaymentsRet(
    @JsonProperty("pay")
    val pay: Pay? = null,

    /**
     * ФИО менеджера, который провел платеж
     */
    @JsonProperty("mgr")
    val manager: String? = null,

    /**
     * Описание менеджера, который провел платеж
     */
    @JsonProperty("mgrdescr")
    val managerDescription: String? = null
)

data class Pay(
    /**
     * Дата/время платежа
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonProperty("paydate")
    val dateTime: LocalDateTime? = null,

    /**
     * Идентификатор платежа
     */
    @JsonProperty("receipt")
    val receipt: String? = null,

    /**
     * Сумма платежа
     */
    @JsonDeserialize(using = AmountDeserializer::class)
    @JsonProperty("amount")
    val amount: BigDecimal? = null
)