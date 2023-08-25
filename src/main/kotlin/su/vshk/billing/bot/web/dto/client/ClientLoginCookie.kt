package su.vshk.billing.bot.web.dto.client

import su.vshk.billing.bot.web.dto.Cookie

data class ClientLoginCookie(
    val uid: Long,
    val cookie: Cookie
)
