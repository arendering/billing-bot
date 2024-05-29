package su.vshk.billing.bot.web.dto.manager

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import su.vshk.billing.bot.util.AmountDeserializer
import java.math.BigDecimal

data class GetAccountRequest(
    @JsonProperty("id")
    val userId: Long? = null
)

data class GetAccountResponse(
    @JsonProperty("ret")
    val ret: GetAccountRet? = null
)

data class GetAccountRet(
    @JsonProperty("account")
    val account: Account? = null,

    @JsonProperty("agreements")
    val agreements: List<AccountAgreement>? = null
)

data class Account(
    @JsonProperty("pass")
    val password: String? = null,

    @JsonProperty("email")
    val email: String? = null
)

data class AccountAgreement(
    @JsonProperty("promisecredit")
    @JsonDeserialize(using = AmountDeserializer::class)
    val promiseCredit: BigDecimal? = null
)