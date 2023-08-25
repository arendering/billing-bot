package su.vshk.billing.bot.util

import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import su.vshk.billing.bot.web.dto.BillingResponseItem
import su.vshk.billing.bot.web.dto.Fault
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import reactor.kotlin.test.test
import su.vshk.billing.bot.util.WebUtils
import su.vshk.billing.bot.util.putTraceId

class WebUtilsTest {

    private val testClient = mock(TestClient::class.java)

    @Test
    fun testRetryIfAuthFailedExecute() {
        mockTestClient()

        WebUtils.retryIfAuthFailedExecute { testClient.doRequest() }
            .putTraceId()
            .test()
            .expectNextMatches {
                assertThat(it.fault).isNull()
                assertThat(it.data).isEqualTo(42)
                true
            }
            .verifyComplete()

        verify(testClient, times(2)).doRequest()
    }

    private fun mockTestClient() {
        whenever(testClient.doRequest())
            .thenReturn(
                BillingResponseItem<Int>(
                    fault = Fault(faultString = "error_auth")
                ).toMono()
            )
            .thenReturn(
                BillingResponseItem(data = 42).toMono()
            )
    }
}

private open class TestClient {
    open fun doRequest(): Mono<BillingResponseItem<Int>> = BillingResponseItem<Int>().toMono()
}