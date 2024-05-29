package su.vshk.billing.bot.web.dto.client

import com.fasterxml.jackson.annotation.JsonProperty

data class ClientLoginRequest(
    @JsonProperty(value = "login")
    val login: String? = null,

    @JsonProperty(value = "pass")
    val password: String? = null
)

data class ClientLoginResponse(
    @JsonProperty(value = "ret")
    val ret: ClientLoginRet? = null
)

data class ClientLoginRet(
    @JsonProperty(value = "uid")
    val userId: Long? = null
)

