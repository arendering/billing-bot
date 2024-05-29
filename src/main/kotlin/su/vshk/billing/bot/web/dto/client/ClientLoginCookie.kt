package su.vshk.billing.bot.web.dto.client

import su.vshk.billing.bot.web.dto.Cookie

data class ClientLoginCookie(
    val userId: Long,
    val cookie: Cookie
)
