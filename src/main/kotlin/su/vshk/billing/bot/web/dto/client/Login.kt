package su.vshk.billing.bot.web.dto.client

import com.fasterxml.jackson.annotation.JsonProperty

data class ClientLoginRequest(
    val login: String? = null,
    @JsonProperty(value = "pass")
    val password: String? = null
)

data class ClientLoginResponse(
    val ret: ClientLoginRet? = null
)

data class ClientLoginRet(
    val uid: Long? = null
)

