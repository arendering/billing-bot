package su.vshk.billing.bot.util

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.util.context.ContextView
import java.util.*

const val TRACE_ID_KEY = "traceId"

inline fun <reified C : Any> C.getLogger(): Logger =
    LoggerFactory.getLogger(this::class.java.name.substringBefore("\$Companion"))

val ContextView.traceId: String get() =
    this[TRACE_ID_KEY]

fun <T> Mono<T>.putTraceId(): Mono<T> =
    this.contextWrite { it.put(TRACE_ID_KEY, UUID.randomUUID().toString()) }

fun <T> Flux<T>.putTraceId(): Flux<T> =
    this.contextWrite { it.put(TRACE_ID_KEY, UUID.randomUUID().toString()) }

fun Logger.debugTraceId(context: ContextView, msg: String) {
    this.debug("$TRACE_ID_KEY: '${context.traceId}' - $msg")
}

fun Logger.infoTraceId(context: ContextView, msg: String) {
    this.info("$TRACE_ID_KEY: '${context.traceId}' - $msg")
}

fun Logger.warnTraceId(context: ContextView, msg: String) {
    this.warn("$TRACE_ID_KEY: '${context.traceId}' - $msg")
}

fun Logger.errorTraceId(context: ContextView, msg: String, ex: Throwable) {
    this.error("$TRACE_ID_KEY: '${context.traceId}' - $msg", ex)
}

fun Logger.errorTraceId(context: ContextView, msg: String) {
    this.error("$TRACE_ID_KEY: '${context.traceId}' - $msg")
}
