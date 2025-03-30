package su.vshk.billing.bot.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import reactor.kotlin.test.test
import su.vshk.billing.bot.web.dto.Cookie
import java.time.Instant

class CookieManagerTest {
    private lateinit var cookieManager: CookieManager

    @BeforeEach
    fun setUp() {
        cookieManager = CookieManager()
    }

    @Test
    fun testClientCookie() {
        val userId = 13L
        cookieManager.getClientCookie(userId)
            .test()
            .expectNextMatches {
                assertThat(it.isEmpty).isTrue
                true
            }
            .verifyComplete()

        cookieManager
            .putClientCookie(
                userId = userId,
                cookie = Cookie(
                    value = "abc123",
                    expTimestampSeconds = Instant.now().epochSecond.plus(20)
                )
            )
            .test()
            .expectNextMatches {
                assertThat(it).isEqualTo("abc123")
                true
            }
            .verifyComplete()

        cookieManager.getClientCookie(userId)
            .test()
            .expectNextMatches {
                assertThat(it.isPresent).isTrue
                assertThat(it.get()).isEqualTo("abc123")
                true
            }
            .verifyComplete()
    }

    @Test
    fun testManagerCookie() {
        cookieManager.getManagerCookie()
            .test()
            .expectNextMatches {
                assertThat(it.isEmpty).isTrue
                true
            }
            .verifyComplete()

        cookieManager
            .putManagerCookie(
                cookie = Cookie(
                    value = "abc123",
                    expTimestampSeconds = Instant.now().epochSecond.plus(20)
                )
            )
            .test()
            .expectNextMatches {
                assertThat(it).isEqualTo("abc123")
                true
            }
            .verifyComplete()

        cookieManager.getManagerCookie()
            .test()
            .expectNextMatches {
                assertThat(it.isPresent).isTrue
                assertThat(it.get()).isEqualTo("abc123")
                true
            }
            .verifyComplete()
    }
}