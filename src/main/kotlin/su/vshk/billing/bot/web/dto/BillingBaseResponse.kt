package su.vshk.billing.bot.web.dto

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseCookie

data class BillingBaseResponse(
    val status: HttpStatus? = null,
    val cookie: ResponseCookie? = null,
    val body: String? = null
)
