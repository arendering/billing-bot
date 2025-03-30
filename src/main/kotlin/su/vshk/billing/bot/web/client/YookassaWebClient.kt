package su.vshk.billing.bot.web.client

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Mono
import su.vshk.billing.bot.config.BotProperties
import su.vshk.billing.bot.util.YookassaBadResponseException
import su.vshk.billing.bot.util.getLogger
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
        private const val IDEMPOTENCE_KEY = "Idempotence-Key"
    }

    private val yookassaProperties = properties.yookassaPayment

    fun createPayment(request: YookassaPayment): Mono<YookassaPayment> =
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
            .doOnSubscribe { logRequestData(request) }
            .flatMap { convertResponse(it) }
            .doOnNext { logResponseData(it) }

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
            throw YookassaBadResponseException(path = properties.yookassaPayment.path!!, msg = "bad status code ${response.rawStatusCode()}")
        }

    private fun logRequestData(request: YookassaPayment) {
        logger.info("${logPrefix()} request body: $request")
    }

    private fun logResponseData(response: YookassaPayment) {
        logger.info("${logPrefix()} response body: $response}")
    }

    private fun logPrefix(): String =
        "Yookassa API ${properties.yookassaPayment.path}"
}