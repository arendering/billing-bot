package su.vshk.billing.bot.dialog

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import reactor.kotlin.core.publisher.toMono
import reactor.kotlin.test.test
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
                        "Установите необходимую сумму обещанного платежа используя виртуальные кнопки.",
                        "Сумма пополнения:",
                        "500 ₽"
                    )
                    .assertCalculatorButtons()

                true
            }
            .verifyComplete()

        assertDialogContainsUser()

        dialogProcessor
            .processOption(request = createRequest(input = PromisePaymentAvailableOptions.AMOUNT_PLUS_ONE_HUNDRED))
            .test()
            .expectNextMatches {
                it.state
                    .assertFlags(isFinishedExpected = false, isCanceledExpected = false)
                    .assertResponseTextContains(
                        "Установите необходимую сумму обещанного платежа используя виртуальные кнопки.",
                        "Сумма пополнения:",
                        "600 ₽"
                    )
                    .assertCalculatorButtons()
                true
            }
            .verifyComplete()

        assertDialogContainsUser()

        dialogProcessor
            .processOption(request = createRequest(input = PromisePaymentAvailableOptions.AMOUNT_MINUS_ONE))
            .test()
            .expectNextMatches {
                it.state
                    .assertFlags(isFinishedExpected = false, isCanceledExpected = false)
                    .assertResponseTextContains(
                        "Установите необходимую сумму обещанного платежа используя виртуальные кнопки.",
                        "Сумма пополнения:",
                        "599 ₽"
                    )
                    .assertCalculatorButtons()
                true
            }
            .verifyComplete()

        assertDialogContainsUser()

        dialogProcessor
            .processOption(request = createRequest(input = PromisePaymentAvailableOptions.AMOUNT_SUBMIT))
            .test()
            .expectNextMatches {
                it.state
                    .assertFlags(isFinishedExpected = true, isCanceledExpected = false)
                    .assertResponseMessageIsNull()
                    .assertOptions(expectedAmount = 599)
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

    @Test
    fun testPromisePaymentUpperBound() {
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

        mockRecommendedPayment(BigDecimal("1450"))
        dialogProcessor
            .processOption(request = createRequest(input = PromisePaymentAvailableOptions.WARNING_APPROVE))
            .test()
            .expectNextMatches {
                it.state
                    .assertFlags(isFinishedExpected = false, isCanceledExpected = false)
                    .assertResponseTextContains(
                        "Установите необходимую сумму обещанного платежа используя виртуальные кнопки.",
                        "Сумма пополнения:",
                        "1450 ₽"
                    )
                    .assertCalculatorButtons()

                true
            }
            .verifyComplete()

        assertDialogContainsUser()

        dialogProcessor
            .processOption(request = createRequest(input = PromisePaymentAvailableOptions.AMOUNT_PLUS_ONE_HUNDRED))
            .test()
            .expectNextMatches {
                it.state
                    .assertFlags(isFinishedExpected = false, isCanceledExpected = false)
                    .assertResponseTextContains(
                        "Установите необходимую сумму обещанного платежа используя виртуальные кнопки.",
                        "Сумма пополнения:",
                        "1450 ₽",
                        "Не может быть больше 1500 ₽"
                    )
                    .assertCalculatorButtons()
                true
            }
            .verifyComplete()

        assertDialogContainsUser()

        dialogProcessor
            .processOption(request = createRequest(input = PromisePaymentAvailableOptions.AMOUNT_SUBMIT))
            .test()
            .expectNextMatches {
                it.state
                    .assertFlags(isFinishedExpected = true, isCanceledExpected = false)
                    .assertResponseMessageIsNull()
                    .assertOptions(expectedAmount = 1450)
                true
            }
            .verifyComplete()

        assertDialogDoesNotContainUser()
    }

    @Test
    fun testPromisePaymentLowerBound() {
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

        mockRecommendedPayment(BigDecimal("50"))
        dialogProcessor
            .processOption(request = createRequest(input = PromisePaymentAvailableOptions.WARNING_APPROVE))
            .test()
            .expectNextMatches {
                it.state
                    .assertFlags(isFinishedExpected = false, isCanceledExpected = false)
                    .assertResponseTextContains(
                        "Установите необходимую сумму обещанного платежа используя виртуальные кнопки.",
                        "Сумма пополнения:",
                        "50 ₽"
                    )
                    .assertCalculatorButtons()

                true
            }
            .verifyComplete()

        assertDialogContainsUser()

        dialogProcessor
            .processOption(request = createRequest(input = PromisePaymentAvailableOptions.AMOUNT_MINUS_ONE_HUNDRED))
            .test()
            .expectNextMatches {
                it.state
                    .assertFlags(isFinishedExpected = false, isCanceledExpected = false)
                    .assertResponseTextContains(
                        "Установите необходимую сумму обещанного платежа используя виртуальные кнопки.",
                        "Сумма пополнения:",
                        "50 ₽",
                        "Не может быть меньше 1 ₽"
                    )
                    .assertCalculatorButtons()
                true
            }
            .verifyComplete()

        assertDialogContainsUser()

        dialogProcessor
            .processOption(request = createRequest(input = PromisePaymentAvailableOptions.AMOUNT_SUBMIT))
            .test()
            .expectNextMatches {
                it.state
                    .assertFlags(isFinishedExpected = true, isCanceledExpected = false)
                    .assertResponseMessageIsNull()
                    .assertOptions(expectedAmount = 50)
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
            .assertButtonLabelsContains("+1 ₽", "-1 ₽", "+25 ₽", "-25 ₽", "+100 ₽", "-100 ₽", "Подключить", "Отмена")
            .assertButtonCallbackDataContains("+1", "-1", "+25", "-25", "+100", "-100", "/submit", "/cancel_amount_step")

    private fun StateDto.assertOptions(expectedAmount: Int): StateDto {
        val options = this.options as PromisePaymentOptions
        assertThat(options.amount).isEqualTo(expectedAmount)
        return this
    }
}