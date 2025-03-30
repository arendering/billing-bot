package su.vshk.billing.bot.dialog

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import reactor.kotlin.core.publisher.toMono
import reactor.kotlin.test.test
import su.vshk.billing.bot.dao.model.Command
import su.vshk.billing.bot.dialog.dto.StateDto
import su.vshk.billing.bot.dialog.option.YookassaPaymentOptions
import su.vshk.billing.bot.web.dto.manager.*
import java.math.BigDecimal

class YookassaPaymentDialogTest: BaseDialogTest() {

    private val command = Command.YOOKASSA_PAYMENT

    @Test
    fun testInvalidCustomer() {
        mockGetAccount(email = "", mobile = null)

        dialogProcessor
            .startDialog(
                request = createRequest(input = command.value),
                user = createUser(),
                command = command
            )
            .test()
            .expectNextMatches {
                it.state
                    .assertFlags(isFinishedExpected = false, isCanceledExpected = true)
                    .assertResponseTextContains(
                        "В Вашей учётной записи отсутствуют необходимые данные для проведения оплаты",
                        "Нажмите сюда для заполнения данных в личном кабинете"
                    )
                true
            }
            .verifyComplete()

        assertDialogDoesNotContainUser()
    }

    @Test
    fun testAddDigit() {
        mockGetAccount()
        mockGetRecommendedPayment(BigDecimal("100"))

        dialogProcessor
            .startDialog(
                request = createRequest(input = command.value),
                user = createUser(),
                command = command
            )
            .test()
            .expectNextMatches {
                it.state
                    .assertFlags(isFinishedExpected = false, isCanceledExpected = false)
                    .assertResponseTextContains(
                        "Установите необходимую сумму",
                        "Сумма пополнения",
                        "100 ₽"
                    )
                    .assertCalculatorButtons()
                true
            }
            .verifyComplete()

        assertDialogContainsUser()

        dialogProcessor
            .processOption(
                createRequest(input = "0")
            )
            .test()
            .expectNextMatches {
                it.state
                    .assertFlags(isFinishedExpected = false, isCanceledExpected = false)
                    .assertResponseTextContains(
                        "Установите необходимую сумму",
                        "Сумма пополнения",
                        "1 000 ₽"
                    )
                    .assertCalculatorButtons()
                true
            }
            .verifyComplete()

        assertDialogContainsUser()

        dialogProcessor
            .processOption(
                createRequest(input = "enter")
            )
            .test()
            .expectNextMatches {
                it.state
                    .assertFlags(isFinishedExpected = true, isCanceledExpected = false)
                    .assertOptions(amount = 1000, email = "foo@bar.com", phone = "79991112233")
                true
            }
            .verifyComplete()

        assertDialogDoesNotContainUser()
    }

    @Test
    fun testEraseDigit() {
        mockGetAccount()
        mockGetRecommendedPayment(BigDecimal("100"))

        dialogProcessor
            .startDialog(
                request = createRequest(input = command.value),
                user = createUser(),
                command = command
            )
            .test()
            .expectNextMatches {
                it.state
                    .assertFlags(isFinishedExpected = false, isCanceledExpected = false)
                    .assertResponseTextContains(
                        "Установите необходимую сумму",
                        "Сумма пополнения",
                        "100 ₽"
                    )
                    .assertCalculatorButtons()
                true
            }
            .verifyComplete()

        assertDialogContainsUser()

        dialogProcessor
            .processOption(
                createRequest(input = "erase")
            )
            .test()
            .expectNextMatches {
                it.state
                    .assertFlags(isFinishedExpected = false, isCanceledExpected = false)
                    .assertResponseTextContains(
                        "Установите необходимую сумму",
                        "Сумма пополнения",
                        "10 ₽"
                    )
                    .assertCalculatorButtons()
                true
            }
            .verifyComplete()

        assertDialogContainsUser()

        dialogProcessor
            .processOption(
                createRequest(input = "enter")
            )
            .test()
            .expectNextMatches {
                it.state
                    .assertFlags(isFinishedExpected = true, isCanceledExpected = false)
                    .assertOptions(amount = 10, email = "foo@bar.com", phone = "79991112233")
                true
            }
            .verifyComplete()

        assertDialogDoesNotContainUser()
    }

    private fun mockGetAccount(
        email: String? = "foo@bar.com",
        mobile: String? = "+79991112233"
    ) {
        whenever(
            billingWebClient.getAccount(any())
        ).thenReturn(
            GetAccountResponse(
                ret = GetAccountRet(
                    account = Account(
                        email = email,
                        mobile = mobile
                    )
                )
            ).toMono()
        )
    }

    private fun mockGetRecommendedPayment(amount: BigDecimal) {
        whenever(
            billingWebClient.getRecommendedPayment(any())
        ).thenReturn(
            GetRecommendedPaymentResponse(amount).toMono()
        )
    }

    private fun StateDto.assertCalculatorButtons(): StateDto {
        this
            .assertButtonLabelsContains("7", "8", "9", "4", "5", "6", "1", "2", "3", "⌫", "0", "AC", "Готово", "⬅ Вернуться в главное меню")
            .assertButtonCallbackDataContains("7", "8", "9", "4", "5", "6", "1", "2", "3", "erase", "0", "clear", "enter", "cancel_amount_step")

        return this
    }

    private fun StateDto.assertOptions(amount: Int, email: String, phone: String): StateDto {
        val options = this.options as YookassaPaymentOptions
        assertThat(options.amount).isEqualTo(amount)
        assertThat(options.customer?.email).isEqualTo(email)
        assertThat(options.customer?.phone).isEqualTo(phone)
        return this
    }
}