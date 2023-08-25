package su.vshk.billing.bot.web.converter

import org.assertj.core.api.Assertions.assertThat
import su.vshk.billing.bot.util.getLogger
import su.vshk.billing.bot.web.client.BillingMethod
import su.vshk.billing.bot.web.dto.manager.GetPaymentsFlt
import su.vshk.billing.bot.web.dto.manager.GetPaymentsRequest
import su.vshk.billing.bot.web.dto.manager.LoginRequest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate

@SpringBootTest
@ActiveProfiles("test")
class RequestConverterTest {

    companion object {
        private val log = getLogger()
    }

    @Autowired
    private lateinit var requestConverter: RequestConverter

    @Test
    fun testManagerLoginRequest() {
        val request = LoginRequest(login = "admin", password = "1234")
        val converted = requestConverter.convert(method = BillingMethod.MANAGER_LOGIN, payload = request)

        log.info(converted)
        assertThat(converted).contains("<urn:Login><login>admin</login><pass>1234</pass></urn:Login>")
    }

    @Test
    fun testGetPaymentsRequest() {
        val request = GetPaymentsRequest(
            flt = GetPaymentsFlt(
                agrmId = 42L,
                dateFrom = LocalDate.of(2010, 6, 12),
                dateTo = LocalDate.of(2020, 10, 30)
            )
        )
        val converted = requestConverter.convert(method = BillingMethod.GET_PAYMENTS, payload = request)

        log.info(converted)
        assertThat(converted).contains("<urn:getPayments>")
        assertThat(converted).contains("</urn:getPayments>")
        assertThat(converted).contains("<agrmid>42</agrmid>")
        assertThat(converted).contains("<dtfrom>2010-06-12</dtfrom>")
        assertThat(converted).contains("<dtto>2020-10-30</dtto>")
    }
}