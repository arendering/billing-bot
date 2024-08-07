package su.vshk.billing.bot.dialog

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import reactor.kotlin.core.publisher.toMono
import reactor.kotlin.test.test
import su.vshk.billing.bot.dao.model.CalculatorButton
import su.vshk.billing.bot.dao.model.Command
import su.vshk.billing.bot.dialog.dto.StateDto
import su.vshk.billing.bot.dialog.option.PromisePaymentAvailableOptions
import su.vshk.billing.bot.dialog.option.PromisePaymentOptions
import su.vshk.billing.bot.web.dto.manager.GetRecommendedPaymentResponse
import java.math.BigDecimal

class PromisePaymentDialogTest: BaseDialogTest() {

    private val command = Command.PROMISE_PAYMENT

    @Test
    fun testPromisePaymentOk() {
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
                        "Предупреждение",
                        "Обещанный платеж выдается сроком на 5 календарных дней",
                        "Отвечая положительно, Вы соглашаетесь с условиями оказания услуги"
                    )
                    .assertButtonLabelsContains("Согласен/согласна", "⬅ Вернуться в главное меню")
                    .assertButtonCallbackDataContains("/approve", "/cancel")

                true
            }
            .verifyComplete()

        assertDialogContainsUser()

        mockRecommendedPayment(BigDecimal("500"))
        dialogProcessor
            .processOption(request = createRequest(input = PromisePaymentAvailableOptions.WARNING_APPROVE))
            .test()
            .expectNextMatches {
                it.state
                    .assertFlags(isFinishedExpected = false, isCanceledExpected = false)
                    .assertResponseTextContains(
                        "Установите необходимую сумму",
                        "Сумма пополнения",
                        "500 ₽"
                    )
                    .assertCalculatorButtons()

                true
            }
            .verifyComplete()

        assertDialogContainsUser()

        dialogProcessor
            .processOption(request = createRequest(input = CalculatorButton.ENTER))
            .test()
            .expectNextMatches {
                it.state
                    .assertFlags(isFinishedExpected = true, isCanceledExpected = false)
                    .assertResponseMessageIsNull()
                    .assertOptions(expectedAmount = 500)
                true
            }
            .verifyComplete()

        assertDialogDoesNotContainUser()
    }

    @Test
    fun testPromisePaymentDebtsOverdue() {
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
                        "Предупреждение",
                        "Обещанный платеж выдается сроком на 5 календарных дней",
                        "Отвечая положительно, Вы соглашаетесь с условиями оказания услуги"
                    )
                    .assertButtonLabelsContains("Согласен/согласна", "⬅ Вернуться в главное меню")
                    .assertButtonCallbackDataContains("/approve", "/cancel")

                true
            }
            .verifyComplete()

        assertDialogContainsUser()

        mockRecommendedPayment(BigDecimal("2000"))
        dialogProcessor
            .processOption(request = createRequest(input = PromisePaymentAvailableOptions.WARNING_APPROVE))
            .test()
            .expectNextMatches {
                it.state
                    .assertFlags(isFinishedExpected = false, isCanceledExpected = true)
                    .assertResponseTextContains("Задолженность на счете превышает максимальную сумму обещанного платежа. Функция недоступна.")
                    .assertButtonLabelsContains("⬅ Вернуться в главное меню")
                    .assertButtonCallbackDataContains("/menu")

                true
            }
            .verifyComplete()

        assertDialogDoesNotContainUser()
    }

    private fun mockRecommendedPayment(amount: BigDecimal) {
        whenever(
            billingWebClient.getRecommendedPayment(any())
        ).thenReturn(
            GetRecommendedPaymentResponse(amount = amount).toMono()
        )
    }

    private fun StateDto.assertCalculatorButtons(): StateDto =
        this
            .assertButtonLabelsContains("7", "8", "9", "4", "5", "6", "1", "2", "3", "⌫", "0", "AC", "Готово", "⬅ Вернуться в главное меню")
            .assertButtonCallbackDataContains("7", "8", "9", "4", "5", "6", "1", "2", "3", "erase", "0", "clear", "enter", "cancel_amount_step")

    private fun StateDto.assertOptions(expectedAmount: Int): StateDto {
        val options = this.options as PromisePaymentOptions
        assertThat(options.amount).isEqualTo(expectedAmount)
        return this
    }
}