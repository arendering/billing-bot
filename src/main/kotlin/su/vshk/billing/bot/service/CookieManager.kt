package su.vshk.billing.bot.service

import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import su.vshk.billing.bot.util.getLogger
import su.vshk.billing.bot.web.dto.Cookie
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

@Service
class CookieManager {
    // Ключ - userId, значение - клиентская кука
    private val clientCookies = ConcurrentHashMap<Long, Cookie>()
    private val managerCookie = AtomicReference<Cookie>()
    private val logger = getLogger()

    fun getClientCookie(userId: Long): Mono<Optional<String>> =
        Mono.fromCallable {
            val cookie = clientCookies[userId]?.let { resolveActual(it) }
            logger.debug("Get client cookie from cache: userId '$userId', cookie '$cookie'")
            Optional.ofNullable(cookie)
        }

    fun putClientCookie(userId: Long, cookie: Cookie): Mono<String> =
        Mono.fromCallable {
            clientCookies[userId] = cookie
            logger.debug("Put client cookie into cache: userId '$userId', cookie '$cookie'")
            cookie.value
        }

    fun removeClientCookie(userId: Long): Mono<Optional<String>> =
        Mono.fromCallable {
            val cookie = clientCookies.remove(userId)
            logger.debug("Remove client cookie from cache: userId '$userId', cookie '$cookie'")
            Optional.ofNullable(cookie?.value)
        }

    fun getManagerCookie(): Mono<Optional<String>> =
        Mono.fromCallable {
            val cookie = managerCookie.get()?.let { resolveActual(it) }
            logger.debug("Get manager cookie from cache: cookie '$cookie'")
            Optional.ofNullable(cookie)
        }

    fun putManagerCookie(cookie: Cookie): Mono<String> =
        Mono.fromCallable {
            managerCookie.set(cookie)
            logger.debug("Put manager cookie into cache: cookie '$cookie'")
            cookie.value
        }

    private fun resolveActual(cookie: Cookie): String? =
        if (Instant.ofEpochSecond(cookie.expTimestampSeconds).isAfter(Instant.now())) {
            cookie.value
        } else {
            null
        }
}