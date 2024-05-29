package su.vshk.billing.bot.web.dto.manager

import com.fasterxml.jackson.annotation.JsonProperty

data class LoginRequest(
    @JsonProperty(value = "login")
    val login: String? = null,

    @JsonProperty(value = "pass")
    val password: String? = null
)

data class LoginResponse(
    @JsonProperty(value = "ret")
    val ret: LoginResponseRet? = null
)

data class LoginResponseRet(
    @JsonProperty(value = "manager")
    val manager: LoginResponseManager? = null
)

data class LoginResponseManager(
    @JsonProperty(value = "personid")
    val personId: Long? = null
)