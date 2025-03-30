package su.vshk.billing.bot.web.client

import su.vshk.billing.bot.util.getLogger
import su.vshk.billing.bot.web.dto.BillingBaseResponse
import org.springframework.http.MediaType
import org.springframework.http.ResponseCookie
import org.springframework.util.MultiValueMap
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Mono
import su.vshk.billing.bot.config.BotProperties
import su.vshk.billing.bot.util.BillingBadResponseException
import java.time.Duration

abstract class BillingBaseWebClient(
    private val webClient: WebClient,
    protected val properties: BotProperties
) {
    companion object {
        private val logger = getLogger()
        private const val COOKIE_KEY = "sessnum"
    }

    protected fun doRequest(body: String, cookie: String? = null): Mono<BillingBaseResponse> =
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
            .doOnSubscribe { logRequestData(body = body, cookie = cookie) }
            .flatMap { response ->
                response.bodyToMono(String::class.java)
                    .map { responseBody ->
                        val responseCookie =
                            if (cookie == null) {
                                resolveResponseCookie(response.cookies())
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
            .doOnNext { logResponseData(it) }

    private fun logRequestData(body: String, cookie: String?) {
        cookie?.let { logger.info("Request cookie: '$it'") }
        logger.info("Request body: '$body'")
    }

    private fun logResponseData(responseData: BillingBaseResponse) {
        logger.info("Response status: '${responseData.status?.value()}'")
        responseData.cookie?.let {
            logger.info("Response cookie: value - '${it.value}', maxAge seconds - '${it.maxAge.seconds}'")
        }
        logger.info("Response body: '${responseData.body}'")
    }

    private fun buildUri(props: BotProperties.WebClientProperties): String =
        UriComponentsBuilder.newInstance()
            .scheme(props.scheme)
            .host(props.host)
            .port(props.port)
            .build()
            .toUriString()

    private fun resolveResponseCookie(responseCookies: MultiValueMap<String, ResponseCookie>): ResponseCookie {
        val cookies = responseCookies[COOKIE_KEY]

        var cookie = cookies?.find { it.domain == properties.webClient.host!! && it.maxAge.isPositive() }
        if (cookie != null) {
            logger.debug("Resolve response cookie by domain and positive max age")
            return cookie
        }

        cookie = cookies?.find { it.maxAge.isPositive() }
        if (cookie != null) {
            logger.debug("Resolve response cookie by positive max age")
            return cookie
        }

        throw BillingBadResponseException("unable to resolve response cookie")
    }

    private fun Duration.isPositive(): Boolean =
        !this.isNegative && !this.isZero
}