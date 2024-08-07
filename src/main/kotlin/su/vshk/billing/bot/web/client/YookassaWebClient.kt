package su.vshk.billing.bot.web.client

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Mono
import reactor.util.context.ContextView
import su.vshk.billing.bot.config.BotProperties
import su.vshk.billing.bot.util.getLogger
import su.vshk.billing.bot.util.infoTraceId
import su.vshk.billing.bot.web.dto.yookassa.YookassaPayment
import java.nio.charset.StandardCharsets
import java.util.*

@Service
class YookassaWebClient(
    private val webClient: WebClient,
    private val objectMapper: ObjectMapper,
    private val properties: BotProperties
) {

    companion object {
        private val logger = getLogger()
        const val IDEMPOTENCE_KEY = "Idempotence-Key"
    }

    fun createPayment(request: YookassaPayment): Mono<YookassaPayment> =
        Mono.deferContextual { context ->
            val yookassaProperties = properties.yookassaPayment

            webClient
                .post()
                .uri(buildUri(yookassaProperties))
                .headers { headers ->
                    headers.contentType = MediaType.APPLICATION_JSON
                    headers.accept = listOf(MediaType.APPLICATION_JSON)
                    headers.setBasicAuth(yookassaProperties.shopId!!, yookassaProperties.secretKey!!, StandardCharsets.UTF_8)
                    headers.set(IDEMPOTENCE_KEY, UUID.randomUUID().toString())
                }
                .bodyValue(convertRequest(request))
                .exchange()
                .doOnSubscribe { logRequestData(context, request) }
                .flatMap { convertResponse(it) }
                .doOnNext { logResponseData(context, it) }
        }

    private fun buildUri(properties: BotProperties.YookassaPaymentProperties): String =
        UriComponentsBuilder.newInstance()
            .scheme(properties.scheme)
            .host(properties.host)
            .port(properties.port)
            .path(properties.path!!)
            .toUriString()

    private fun convertRequest(request: YookassaPayment): String =
        objectMapper.writeValueAsString(request)

    private fun convertResponse(response: ClientResponse): Mono<YookassaPayment> =
        if (response.statusCode().is2xxSuccessful) {
            response.bodyToMono(String::class.java)
                .map { objectMapper.readValue(it, YookassaPayment::class.java) }
        } else {
            throw RuntimeException("${logPrefix()} bad status code: ${response.rawStatusCode()}")
        }

    private fun logRequestData(context: ContextView, request: YookassaPayment) {
        logger.infoTraceId(context, "${logPrefix()} request body: $request")
    }

    private fun logResponseData(context: ContextView, response: YookassaPayment) {
        logger.infoTraceId(context, "${logPrefix()} response body: $response}")
    }

    private fun logPrefix(): String =
        "yookassa API ${properties.yookassaPayment.path}"
}