package su.vshk.billing.bot.service

import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import su.vshk.billing.bot.util.debugTraceId
import su.vshk.billing.bot.util.getLogger
import su.vshk.billing.bot.web.dto.Cookie
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

@Service
class CookieManager {
    // Ключ - uid, значение - клиентская кука
    private val clientCookies = ConcurrentHashMap<Long, Cookie>()
    private val managerCookie = AtomicReference<Cookie>()
    private val log = getLogger()

    fun getClientCookie(uid: Long): Mono<Optional<String>> =
        Mono.deferContextual { contextView ->
            Mono.fromCallable {
                val cookie = clientCookies[uid]?.let { resolveActual(it) }
                log.debugTraceId(context = contextView, msg = "get client cookie from cache: uid '$uid', cookie '$cookie'")
                Optional.ofNullable(cookie)
            }
        }

    fun putClientCookie(uid: Long, cookie: Cookie): Mono<String> =
        Mono.deferContextual { contextView ->
            Mono.fromCallable {
                clientCookies[uid] = cookie
                log.debugTraceId(context = contextView, msg = "put client cookie in cache: uid '$uid', cookie '$cookie'")
                cookie.value
            }
        }

    fun removeClientCookie(uid: Long): Mono<Optional<String>> =
        Mono.deferContextual { contextView ->
            Mono.fromCallable {
                val cookie = clientCookies.remove(uid)
                log.debugTraceId(context = contextView, msg = "remove client cookie from cache: uid '$uid', cookie '$cookie'")
                Optional.ofNullable(cookie?.value)
            }
        }

    fun getManagerCookie(): Mono<Optional<String>> =
        Mono.deferContextual { contextView ->
            Mono.fromCallable {
                val cookie = managerCookie.get()?.let { resolveActual(it) }
                log.debugTraceId(context = contextView, msg = "get manager cookie from cache: cookie '$cookie'")
                Optional.ofNullable(cookie)
            }
        }

    fun putManagerCookie(cookie: Cookie): Mono<String> =
        Mono.deferContextual { contextView ->
            Mono.fromCallable {
                managerCookie.set(cookie)
                log.debugTraceId(context = contextView, msg = "put manager cookie in cache: cookie '$cookie'")
                cookie.value
            }
        }

    private fun resolveActual(cookie: Cookie): String? =
        if (Instant.ofEpochSecond(cookie.expTimestampSeconds).isAfter(Instant.now())) {
            cookie.value
        } else {
            null
        }
}