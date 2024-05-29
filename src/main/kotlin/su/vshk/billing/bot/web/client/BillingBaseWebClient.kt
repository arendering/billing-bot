package su.vshk.billing.bot.web.client

import su.vshk.billing.bot.util.getLogger
import su.vshk.billing.bot.util.infoTraceId
import su.vshk.billing.bot.web.dto.BillingBaseResponse
import org.springframework.http.MediaType
import org.springframework.http.ResponseCookie
import org.springframework.util.MultiValueMap
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Mono
import reactor.util.context.ContextView
import su.vshk.billing.bot.config.BotProperties
import su.vshk.billing.bot.util.debugTraceId
import java.time.Duration

abstract class BillingBaseWebClient(
    private val webClient: WebClient,
    protected val properties: BotProperties
) {
    companion object {
        private val log = getLogger()
        private const val COOKIE_KEY = "sessnum"
    }

    protected fun doRequest(body: String, cookie: String? = null): Mono<BillingBaseResponse> =
        Mono.deferContextual { context ->
            webClient
                .post()
                .uri(buildUri(properties.webClient))
                .contentType(MediaType.APPLICATION_XML)
                .accept(MediaType.APPLICATION_XML)
                .cookies { cookies ->
                    cookie?.let { cookies.set(COOKIE_KEY, it) }
                }
                .bodyValue(body)
                .exchange() //TODO: refactoring
                .doOnSubscribe { logRequestData(context = context, body = body, cookie = cookie) }
                .flatMap { response ->
                    response.bodyToMono(String::class.java)
                        .map { responseBody ->
                            val responseCookie =
                                if (cookie == null) {
                                    resolveResponseCookie(context = context, domain = properties.webClient.host!!, responseCookies = response.cookies())
                                } else {
                                    null
                                }

                            BillingBaseResponse(
                                status = response.statusCode(),
                                cookie = responseCookie,
                                body = responseBody
                            )
                        }
                }
                .doOnNext { logResponseData(context = context, responseData = it) }
        }

    private fun logRequestData(context: ContextView, body: String, cookie: String?) {
        cookie?.let { log.infoTraceId(context, "request cookie: '$it'") }
        log.infoTraceId(context, "request body: '$body'")
    }

    private fun logResponseData(context: ContextView, responseData: BillingBaseResponse) {
        log.infoTraceId(context, "response status: '${responseData.status?.value()}'")
        responseData.cookie?.let {
            log.infoTraceId(context, "response cookie: value - '${it.value}', maxAge seconds - '${it.maxAge.seconds}'")
        }
        log.infoTraceId(context, "response body: '${responseData.body}'")
    }

    private fun buildUri(props: BotProperties.WebClientProperties): String =
        UriComponentsBuilder.newInstance()
            .scheme(props.scheme)
            .host(props.host)
            .port(props.port)
            .build()
            .toUriString()

    private fun resolveResponseCookie(context: ContextView, domain: String, responseCookies: MultiValueMap<String, ResponseCookie>): ResponseCookie {
        val cookies = responseCookies[COOKIE_KEY]

        var cookie = cookies?.find { it.domain == domain && it.maxAge.isPositive() }
        if (cookie != null) {
            log.debugTraceId(context = context, msg = "resolve response cookie by domain and positive max age")
            return cookie
        }

        cookie = cookies?.find { it.maxAge.isPositive() }
        if (cookie != null) {
            log.debugTraceId(context = context, msg = "resolve response cookie by positive max age")
            return cookie
        }

        throw RuntimeException("unable to resolve response cookie")
    }

    private fun Duration.isPositive(): Boolean =
        !this.isNegative && !this.isZero
}