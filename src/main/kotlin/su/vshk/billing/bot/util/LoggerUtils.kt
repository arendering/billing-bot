package su.vshk.billing.bot.util

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import reactor.core.publisher.Mono
import reactor.util.context.ContextView
import java.util.*

const val BOT_TRACE_ID_KEY = "bot-trace-id"

inline fun <reified C : Any> C.getLogger(): Logger =
    LoggerFactory.getLogger(this::class.java.name.substringBefore("\$Companion"))

fun <T> runWithMdcContext(
    botTraceId: String? = UUID.randomUUID().toString(),
    rx: Mono<T>
): Mono<T> =
    Mono
        .deferContextual { context ->
            MDC.put(BOT_TRACE_ID_KEY, context.botTraceId)
            rx
        }
        .contextWrite { it.put(BOT_TRACE_ID_KEY, botTraceId ?: UUID.randomUUID().toString()) }

val ContextView.botTraceId: String? get() =
    this.getOrDefault<String>(BOT_TRACE_ID_KEY, null)
