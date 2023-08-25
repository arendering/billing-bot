package su.vshk.billing.bot.message

import org.assertj.core.api.Assertions.assertThat
import su.vshk.billing.bot.service.dto.InfoDto
import su.vshk.billing.bot.service.dto.PaymentsDto
import su.vshk.billing.bot.util.getLogger
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import su.vshk.billing.bot.message.ResponseMessageContentFormatter
import su.vshk.billing.bot.web.dto.manager.Account
import su.vshk.billing.bot.web.dto.manager.AccountAgreement
import su.vshk.billing.bot.web.dto.manager.GetAccountRet
import su.vshk.billing.bot.web.dto.manager.GetVgroupsRet
import java.math.BigDecimal

@SpringBootTest
@ActiveProfiles("test")
class ResponseMessageContentFormatterTest {

    @Autowired
    private lateinit var responseMessageContentFormatter: ResponseMessageContentFormatter

    private val log = getLogger()

    @Test
    fun testInfoMessage() {
        val getVgroupsRet = GetVgroupsRet(
            username = "Иванов И.И",
            agrmNum = "1234",
            balance = BigDecimal("-92.06")
        )

        val getAccountRet = GetAccountRet(
            account = Account(
                email = "foobar@mail.ru"
            ),
            agreements = listOf(
                AccountAgreement(
                    promiseCredit = BigDecimal("1500.15")
                )
            )
        )

        val msg = responseMessageContentFormatter.info(
            InfoDto(
                getVgroupsRet = getVgroupsRet,
                defaultRecommendedPayment = BigDecimal("300"),
                actualRecommendedPayment = BigDecimal("500"),
                getAccountRet = getAccountRet
            )
        ).text

        log.info("message - $msg")
        assertThat(msg).contains("ФИО")
        assertThat(msg).contains("Иванов И.И")
        assertThat(msg).contains("Номер договора")
        assertThat(msg).contains("1234")
        assertThat(msg).contains("Баланс")
        assertThat(msg).contains("-92.06 ₽")
        assertThat(msg).contains("Ежемесячный платеж")
        assertThat(msg).contains("300 ₽")
        assertThat(msg).contains("Рекомендуется внести")
        assertThat(msg).contains("500 ₽")
        assertThat(msg).contains("Подключен обещанный платеж")
        assertThat(msg).contains("1 500.15 ₽")
        assertThat(msg).contains("E-mail")
        assertThat(msg).contains("foobar@mail.ru")
    }

    @Test
    fun testGetInfoEmptyFieldMessage() {
        val getVgroupsRet = GetVgroupsRet(
            username = "Иванов И.И",
            agrmId = 42L,
            agrmNum = "1234",
        )

        val getAccountRet = GetAccountRet(
            account = Account(
                email = "foobar@mail.ru"
            ),
            agreements = emptyList()
        )

        val msg = responseMessageContentFormatter.info(
            InfoDto(
                getVgroupsRet = getVgroupsRet,
                defaultRecommendedPayment = BigDecimal("300"),
                actualRecommendedPayment = BigDecimal("500"),
                getAccountRet = getAccountRet
            )
        ).text

        log.info("message - $msg")
        assertThat(msg).contains("ФИО")
        assertThat(msg).contains("Иванов И.И")
        assertThat(msg).contains("Номер договора")
        assertThat(msg).contains("1234")
        assertThat(msg).doesNotContain("Баланс:")
        assertThat(msg).contains("Ежемесячный платеж")
        assertThat(msg).contains("300 ₽")
        assertThat(msg).contains("Рекомендуется внести")
        assertThat(msg).contains("500 ₽")
        assertThat(msg).doesNotContain("Подключен обещанный платеж")
        assertThat(msg).contains("E-mail")
        assertThat(msg).contains("foobar@mail.ru")
    }

    @Test
    fun testGetEmptyPaymentsMessage() {
        val msg = responseMessageContentFormatter.payments(
            dto = PaymentsDto(
                dateFrom = "07.01.2010",
                dateTo = "12.10.2020",
                payments = emptyList()
            )
        ).text

        log.info("message - $msg")
        assertThat(msg).contains("Платежи с 07.01.2010 по 12.10.2020")
        assertThat(msg).contains("Нет данных")
    }

    @Test
    fun testGetPaymentsFullMessage() {
        val dto = PaymentsDto(
            dateFrom = "07.01.2010",
            dateTo = "12.10.2020",
            payments = listOf(
                PaymentsDto.PaymentDto(
                    date = "01.06.2015",
                    time = "12:00:00",
                    id = "001-25",
                    amount = "1 000.25",
                    manager = "Иванов И.И (ПАО Ромашка)"
                )
            )
        )

        val msg = responseMessageContentFormatter.payments(dto).text

        log.info("message - $msg")
        assertThat(msg).contains("Платежи с 07.01.2010 по 12.10.2020")
        assertThat(msg).contains("Дата:")
        assertThat(msg).contains("01.06.2015")
        assertThat(msg).contains("Время:")
        assertThat(msg).contains("12:00:00")
        assertThat(msg).contains("ID платежа:")
        assertThat(msg).contains("001-25")
        assertThat(msg).contains("Сумма платежа:")
        assertThat(msg).contains("1 000.25 ₽")
        assertThat(msg).contains("Контрагент:")
        assertThat(msg).contains("Иванов И.И (ПАО Ромашка)")
    }
}