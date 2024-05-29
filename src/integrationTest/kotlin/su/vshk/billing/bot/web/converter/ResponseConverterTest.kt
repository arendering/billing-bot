package su.vshk.billing.bot.web.converter

import org.assertj.core.api.Assertions.assertThat
import su.vshk.billing.bot.web.client.BillingMethod
import su.vshk.billing.bot.web.dto.client.ClientLoginResponse
import su.vshk.billing.bot.web.dto.client.ClientPromisePaymentResponse
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.core.io.ClassPathResource
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.reactive.function.client.WebClient
import su.vshk.billing.bot.web.dto.manager.*
import java.time.LocalDateTime

@SpringBootTest
@ActiveProfiles("test")
class ResponseConverterTest {

    @Autowired
    private lateinit var responseConverter: ResponseConverter

    @MockBean
    private lateinit var webClient: WebClient

    @Test
    fun testInvalidManagerCookieResponse() {
        val httpBody = readResourceAsText("/converter/invalid_manager_cookie_response.xml")
        val authErrorResponse = responseConverter.convertFault(httpBody)

        assertThat(authErrorResponse.faultCode).isEqualTo("SOAP-ENV:Server")
        assertThat(authErrorResponse.faultString).isEqualTo("error_auth")
        assertThat(authErrorResponse.detail).isEqualTo("Manager not authorized")
    }

    @Test
    fun testManagerLoginResponse() {
        val response = responseConverter.convert(
            method = BillingMethod.MANAGER_LOGIN,
            httpBody = readResourceAsText("/converter/manager_login_response.xml"),
            clazz = LoginResponse::class.java
        )
        
        assertThat(response.ret?.manager?.personId).isEqualTo(11)
    }

    @Test
    fun testGetVgroupsResponse() {
        val getVgroupsResponse = responseConverter.convert(
            method = BillingMethod.GET_VGROUPS,
            httpBody = readResourceAsText("/converter/get_vgroups_response.xml"),
            clazz = GetVgroupsResponse::class.java
        )

        assertThat(getVgroupsResponse.ret?.size).isEqualTo(4)

        val first = getVgroupsResponse.ret?.first()!!
        assertThat(first.username).isEqualTo("Иванов Иван Иванович")
        assertThat(first.agreementId).isEqualTo(9999L)
        assertThat(first.agreementNumber).isEqualTo("Int-1111/11")
        assertThat(first.balance).isEqualTo("486.10")
        assertThat(first.addresses?.size).isEqualTo(3)
        assertThat(first.agentDescription).isEqualTo("Услуги")
        assertThat(first.blocked).isEqualTo(0)
    }

    @Test
    fun testClientLoginErrorReponse() {
        val response = responseConverter.convertFault(readResourceAsText("/converter/client_login_error.xml"))

        assertThat(response.faultCode).isEqualTo("SOAP-ENV:Server")
        assertThat(response.faultString).contains("Invalid login/pass")
        assertThat(response.detail).contains("Invalid login/pass")
    }

    @Test
    fun testClientLoginResponse() {
        val response = responseConverter.convert(
            method = BillingMethod.CLIENT_LOGIN,
            httpBody = readResourceAsText("/converter/client_login_response.xml"),
            clazz = ClientLoginResponse::class.java
        )
        
        assertThat(response.ret?.userId).isEqualTo(1917L)
    }

    @Test
    fun testGetPaymentsEmptyResponse() {
        val response = responseConverter.convert(
            method = BillingMethod.GET_PAYMENTS,
            httpBody = readResourceAsText("/converter/get_payments_empty_response.xml"),
            clazz = GetPaymentsResponse::class.java
        )

        assertThat(response.ret).isNull()
    }

    @Test
    fun testGetPaymentsResponse() {
        val response = responseConverter.convert(
            method = BillingMethod.GET_PAYMENTS,
            httpBody = readResourceAsText("/converter/get_payments_response.xml"),
            clazz = GetPaymentsResponse::class.java
        )

        val ret = response.ret!!
        assertThat(ret.size).isEqualTo(2)

        val payment = ret[0]
        assertThat(payment.manager).isEqualTo("Петров Петр Петрович")
        assertThat(payment.managerDescription).isEqualTo("")

        val pay = payment.pay
        assertThat(pay?.dateTime)
            .isEqualTo(
                LocalDateTime.of(2022, 5, 16, 19, 6, 32)
            )
        assertThat(pay?.receipt).isEqualTo("22222222222222-2222")
        assertThat(pay?.amount).isEqualTo("-6000.00")
    }

    @Test
    fun testSinglePayment() {
        // проверяет, что один элемент корректно десериализуется в список
        val response = responseConverter.convert(
            method = BillingMethod.GET_PAYMENTS,
            httpBody = readResourceAsText("/converter/get_payments_single_element.xml"),
            clazz = GetPaymentsResponse::class.java
        )

        val ret = response.ret!!
        assertThat(ret.size).isEqualTo(1)
    }

    @Test
    fun testGetAccountResponse() {
        val response = responseConverter.convert(
            method = BillingMethod.GET_ACCOUNT,
            httpBody = readResourceAsText("/converter/get_account_response.xml"),
            clazz = GetAccountResponse::class.java
        )

        val account = response.ret?.account!!
        assertThat(account.password).isEqualTo("secret")
        assertThat(account.email).isEqualTo("foobar@mail.ru")

        val agreement = response.ret?.agreements.let {
            assertThat(it!!.size).isEqualTo(1)
            it[0]
        }
        assertThat(agreement.promiseCredit).isEqualTo("1500.50")
    }

    @Test
    fun testClientPromisePaymentResponse() {
        val response = responseConverter.convert(
            method = BillingMethod.CLIENT_PROMISE_PAYMENT,
            httpBody = readResourceAsText("/converter/client_promise_payment_response.xml"),
            clazz = ClientPromisePaymentResponse::class.java
        )

        assertThat(response.ret).isEqualTo(1L)
    }

    @Test
    fun testGetRecommendedPaymentResponse() {
        val response = responseConverter.convert(
            method = BillingMethod.GET_RECOMMENDED_PAYMENT,
            httpBody = readResourceAsText("/converter/get_recommended_payment_response.xml"),
            clazz = GetRecommendedPaymentResponse::class.java
        )
        assertThat(response.amount).isEqualTo("42.13")
    }

    @Test
    fun testGetSbssKnowledge() {
        val response = responseConverter.convert(
            method = BillingMethod.GET_SBSS_KNOWLEDGE,
            httpBody = readResourceAsText("/converter/get_sbss_knowledge_response.xml"),
            clazz = GetSbssKnowledgeResponse::class.java
        )

        val texts = response.ret?.posts?.mapNotNull { it.post?.text }
        assertThat(texts).isNotNull
        assertThat(texts!!.size).isEqualTo(4)
    }

    private fun readResourceAsText(path: String): String =
        ClassPathResource(path).inputStream.bufferedReader().use { it.readText() }
}