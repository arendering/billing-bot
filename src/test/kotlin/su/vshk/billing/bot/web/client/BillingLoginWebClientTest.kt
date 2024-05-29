package su.vshk.billing.bot.web.client

import com.nhaarman.mockitokotlin2.*
import org.assertj.core.api.Assertions.assertThat
import su.vshk.billing.bot.util.putTraceId
import su.vshk.billing.bot.web.converter.RequestConverter
import su.vshk.billing.bot.web.converter.ResponseConverter
import su.vshk.billing.bot.web.dto.client.ClientLoginRequest
import su.vshk.billing.bot.web.dto.client.ClientLoginResponse
import su.vshk.billing.bot.web.dto.client.ClientLoginRet
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseCookie
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClient.RequestBodySpec
import org.springframework.web.reactive.function.client.WebClient.RequestBodyUriSpec
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersSpec
import reactor.kotlin.core.publisher.toMono
import reactor.kotlin.test.test
import su.vshk.billing.bot.config.BotProperties
import su.vshk.billing.bot.web.dto.manager.LoginRequest
import su.vshk.billing.bot.web.dto.manager.LoginResponse

@ExtendWith(MockitoExtension::class)
class BillingLoginWebClientTest {

    @Mock
    private lateinit var webClient: WebClient

    @Mock
    private lateinit var requestHeadersSpec: RequestHeadersSpec<*>

    @Mock
    private lateinit var requestBodySpec: RequestBodySpec

    @Mock
    private lateinit var requestBodyUriSpec: RequestBodyUriSpec

    @Mock
    private lateinit var clientResponse: ClientResponse

    @Mock
    private lateinit var requestConverter: RequestConverter

    @Mock
    private lateinit var responseConverter: ResponseConverter

    private lateinit var subj: BillingLoginWebClient

    @BeforeEach
    fun setUp() {
        subj = BillingLoginWebClient(
            webClient = webClient,
            properties = BotProperties(
                webClient = BotProperties.WebClientProperties(
                    scheme = "http",
                    host = "10.10.10.10",
                    port = "8888",
                    manager = BotProperties.ManagerCredentials(
                        login = "admin",
                        password = "1234"
                    )
                )
            ),
            requestConverter = requestConverter,
            responseConverter = responseConverter
        )
    }

    @Test
    fun testGetClientId() {
        mockClientConvertRequest(requestBody = "some_request_body")

        mockWebClientCall(
            responseStatus = HttpStatus.OK,
            responseCookie = ResponseCookie.from("sessnum", "1234abcd").maxAge(1800L).build(),
            responseBody = "some_response_body"
        )

        mockClientConvertResponse(
            ClientLoginResponse(
                ret = ClientLoginRet(
                    userId = 1917L
                )
            )
        )

        val request = ClientLoginRequest(login = "user", password = "1234")

        subj.clientLogin(request)
            .putTraceId()
            .test()
            .expectNextMatches {
                assertThat(it.isPresent).isTrue
                val clientCookie = it.get()
                assertThat(clientCookie.userId).isEqualTo(1917L)
                assertThat(clientCookie.cookie.value).isEqualTo("1234abcd")

                true
            }
            .verifyComplete()

        verify(requestConverter)
            .convert(
                eq("ClientLogin"),
                argThat<ClientLoginRequest> {
                    assertThat(this.login).isEqualTo("user")
                    assertThat(this.password).isEqualTo("1234")
                    true
                }
            )

        verify(responseConverter)
            .convert(
                eq("ClientLogin"),
                eq("some_response_body"),
                any<Class<ClientLoginResponse>>()
            )

        verify(requestBodyUriSpec)
            .uri(eq("http://10.10.10.10:8888"))

        verify(requestBodySpec)
            .bodyValue(eq("some_request_body"))
    }

    @Test
    fun testGetClientCookie() {
        mockClientConvertRequest(requestBody = "some_request_body")

        mockWebClientCall(
            responseStatus = HttpStatus.OK,
            responseCookie = ResponseCookie.from("sessnum", "1234abcd").maxAge(1800L).build(),
            responseBody = "some_response_body"
        )

        mockClientConvertResponse(
            ClientLoginResponse(
                ret = ClientLoginRet(
                    userId = 1917L
                )
            )
        )

        val request = ClientLoginRequest(login = "user", password = "1234")

        subj.getClientCookie(request = request)
            .putTraceId()
            .test()
            .expectNextMatches {
                val cookie = it.get()
                assertThat(cookie.value).isEqualTo("1234abcd")

                true
            }
            .verifyComplete()

        verify(requestConverter).convert(
            eq("ClientLogin"),
            argThat<ClientLoginRequest> {
                assertThat(this.login).isEqualTo("user")
                assertThat(this.password).isEqualTo("1234")
                true
            }
        )

        verify(responseConverter).convert(
            eq("ClientLogin"),
            eq("some_response_body"),
            any<Class<ClientLoginResponse>>()
        )

        verify(requestBodyUriSpec)
            .uri(eq("http://10.10.10.10:8888"))

        verify(requestBodySpec)
            .bodyValue(eq("some_request_body"))
    }

    @Test
    fun testGetManagerCookie() {
        mockManagerConvertRequest(requestBody = "some_request_body")

        mockWebClientCall(
            responseStatus = HttpStatus.OK,
            responseCookie = ResponseCookie.from("sessnum", "1234abcd").maxAge(600L).build(),
            responseBody = "some_response_body"
        )

        mockManagerConvertResponse()

        subj.getManagerCookie()
            .putTraceId()
            .test()
            .expectNextMatches {
                assertThat(it.value).isEqualTo("1234abcd")
                true
            }
            .verifyComplete()

        verify(requestConverter).convert(
            eq("Login"),
            argThat<LoginRequest> {
                assertThat(this.login).isEqualTo("admin")
                assertThat(this.password).isEqualTo("1234")
                true
            }
        )

        verify(responseConverter).convert(
            eq("Login"),
            eq("some_response_body"),
            any<Class<LoginResponse>>()
        )

        verify(requestBodyUriSpec)
            .uri(eq("http://10.10.10.10:8888"))

        verify(requestBodySpec)
            .bodyValue(eq("some_request_body"))
    }

    private fun mockClientConvertRequest(requestBody: String) {
        whenever(
            requestConverter.convert(any(), any<ClientLoginRequest>())
        ).thenReturn(
            requestBody
        )
    }

    private fun mockManagerConvertRequest(requestBody: String) {
        whenever(
            requestConverter.convert(any(), any<LoginRequest>())
        ).thenReturn(
            requestBody
        )
    }

    private fun mockWebClientCall(responseStatus: HttpStatus, responseCookie: ResponseCookie?, responseBody: String) {
        whenever(webClient.post()).thenReturn(requestBodyUriSpec)
        whenever(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec)
        whenever(requestBodySpec.contentType(any())).thenReturn(requestBodySpec)
        whenever(requestBodySpec.accept(any())).thenReturn(requestBodySpec)
        whenever(requestBodySpec.cookies(any())).thenReturn(requestBodySpec)
        whenever(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec)
        whenever(requestHeadersSpec.exchange()).thenReturn(clientResponse.toMono())
        whenever(clientResponse.bodyToMono(any<Class<*>>())).thenReturn(responseBody.toMono())
        whenever(clientResponse.statusCode()).thenReturn(responseStatus)

        val cookies = LinkedMultiValueMap<String, ResponseCookie>()
        responseCookie?.let { cookies["sessnum"] = listOf(it) }
        whenever(clientResponse.cookies()).thenReturn(cookies)
    }

    private fun mockClientConvertResponse(response: ClientLoginResponse) {
        whenever(
            responseConverter.convert(any(), any(), any<Class<ClientLoginResponse>>())
        ).thenReturn(
            response
        )
    }

    private fun mockManagerConvertResponse() {
        whenever(
            responseConverter.convert(any(), any(), any<Class<LoginResponse>>())
        ).thenReturn(
            LoginResponse()
        )
    }
}