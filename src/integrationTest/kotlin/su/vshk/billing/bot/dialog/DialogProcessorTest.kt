package su.vshk.billing.bot.dialog

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argThat
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import su.vshk.billing.bot.dao.model.UserEntity
import su.vshk.billing.bot.web.client.BillingWebClient
import su.vshk.billing.bot.web.dto.manager.GetRecommendedPaymentResponse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import reactor.kotlin.core.publisher.toMono
import reactor.kotlin.test.test
import su.vshk.billing.bot.dao.model.Command
import su.vshk.billing.bot.dialog.option.*
import java.math.BigDecimal

@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DialogProcessorTest {

    @Autowired
    private lateinit var dialogProcessor: DialogProcessor

    @MockBean
    private lateinit var billingWebClient: BillingWebClient

    //TODO: добавь в тесты проверку кнопок

    @Test
    fun testStartOk() {
        val telegramId = 42L
        val user = UserEntity(telegramId = telegramId)
        dialogProcessor.startDialog(user = user, command = Command.START)
            .test()
            .expectNextMatches {
                val state = it.state!!
                assertThat(state.isFinished).isFalse
                assertThat(state.isCancelled).isFalse
                state.responseMessageItem?.content?.text.let { text ->
                    assertThat(text).contains("Добро пожаловать!")
                    assertThat(text).contains("Введите логин:")
                }
                true
            }
            .verifyComplete()

        assertThat(dialogProcessor.contains(telegramId)).isTrue

        dialogProcessor.processOption(telegramId = telegramId, option = "user")
            .test()
            .expectNextMatches {
                val state = it.state!!
                assertThat(state.isFinished).isFalse
                assertThat(state.isCancelled).isFalse
                assertThat(state.responseMessageItem?.content?.text).contains("Введите пароль:")
                true
            }
            .verifyComplete()

        assertThat(dialogProcessor.contains(telegramId)).isTrue

        dialogProcessor.processOption(telegramId = telegramId, option = "1234")
            .test()
            .expectNextMatches {
                assertThat(it.user).isEqualTo(user)
                val state = it.state!!
                assertThat(state.isFinished).isTrue
                assertThat(state.isCancelled).isFalse
                assertThat(state.command).isEqualTo(Command.START)

                val options = state.options as LoginOptions
                assertThat(options.login).isEqualTo("user")
                assertThat(options.password).isEqualTo("1234")

                assertThat(state.responseMessageItem).isNull()
                true
            }
            .verifyComplete()

        assertThat(dialogProcessor.contains(telegramId)).isFalse
    }

    @Test
    fun testPaymentsOk() {
        val telegramId = 42L
        val user = UserEntity(telegramId = telegramId)
        dialogProcessor.startDialog(user = user, command = Command.PAYMENTS)
            .test()
            .expectNextMatches {
                val state = it.state!!
                assertThat(state.isFinished).isFalse
                assertThat(state.isCancelled).isFalse
                assertThat(state.responseMessageItem?.content?.text).contains("За какой период времени Вы хотите увидеть список платежей ?")
                true
            }
            .verifyComplete()

        assertThat(dialogProcessor.contains(telegramId)).isTrue

        dialogProcessor.processOption(telegramId = telegramId, option = PaymentsAvailableOptions.PERIOD_ONE_MONTH)
            .test()
            .expectNextMatches {
                assertThat(it.user).isEqualTo(user)
                val state = it.state!!
                assertThat(state.isFinished).isTrue
                assertThat(state.isCancelled).isFalse
                assertThat(state.command).isEqualTo(Command.PAYMENTS)

                val options = state.options as PaymentsOptions
                assertThat(options.period).isEqualTo(PaymentsPeriod.ONE_MONTH)

                assertThat(state.responseMessageItem).isNull()
                true
            }
            .verifyComplete()

        assertThat(dialogProcessor.contains(telegramId)).isFalse
    }

    @Test
    fun testPaymentsCancel() {
        val telegramId = 42L
        val user = UserEntity(telegramId = telegramId)
        dialogProcessor.startDialog(user = user, command = Command.PAYMENTS)
            .test()
            .expectNextMatches {
                val state = it.state!!
                assertThat(state.isFinished).isFalse
                assertThat(state.isCancelled).isFalse
                assertThat(state.responseMessageItem?.content?.text).contains("За какой период времени Вы хотите увидеть список платежей ?")
                true
            }
            .verifyComplete()

        assertThat(dialogProcessor.contains(telegramId)).isTrue

        dialogProcessor.processOption(telegramId = telegramId, option = GenericAvailableOptions.CANCEL)
            .test()
            .expectNextMatches {
                val state = it.state!!
                assertThat(state.isFinished).isFalse
                assertThat(state.isCancelled).isTrue
                assertThat(state.responseMessageItem?.content?.text).contains("Главное меню")
                true
            }
            .verifyComplete()

        assertThat(dialogProcessor.contains(telegramId)).isFalse
    }

    @Test
    fun testPromisePaymentOk() {
        val telegramId = 42L
        val user = UserEntity(telegramId = telegramId, agrmId = 999L)
        dialogProcessor.startDialog(user = user, command = Command.PROMISE_PAYMENT)
            .test()
            .expectNextMatches {
                val state = it.state!!
                assertThat(state.isFinished).isFalse
                assertThat(state.isCancelled).isFalse
                state.responseMessageItem?.content?.text.let { text ->
                    assertThat(text).contains("Предупреждение")
                    assertThat(text).contains("Обещанный платеж выдается сроком на 5 календарных дней")
                    assertThat(text).contains("Отвечая положительно, Вы соглашаетесь с условиями оказания услуги")
                }
                true
            }
            .verifyComplete()

        assertThat(dialogProcessor.contains(telegramId)).isTrue

        mockGetRecommendedPayment(
            GetRecommendedPaymentResponse(
                ret = BigDecimal("500")
            )
        )
        dialogProcessor.processOption(telegramId = telegramId, option = PromisePaymentAvailableOptions.WARNING_APPROVE)
            .test()
            .expectNextMatches {
                val state = it.state!!
                assertThat(state.isFinished).isFalse
                assertThat(state.isCancelled).isFalse
                state.responseMessageItem?.content?.text.let { text ->
                    assertThat(text).contains("Установите необходимую сумму обещанного платежа используя виртуальные кнопки.")
                    assertThat(text).contains("Сумма пополнения:")
                    assertThat(text).contains("500 ₽")
                }
                true
            }
            .verifyComplete()

        assertThat(dialogProcessor.contains(telegramId)).isTrue

        dialogProcessor.processOption(telegramId = telegramId, option = PromisePaymentAvailableOptions.AMOUNT_PLUS_ONE_HUNDRED)
            .test()
            .expectNextMatches {
                val state = it.state!!
                assertThat(state.isFinished).isFalse
                assertThat(state.isCancelled).isFalse
                state.responseMessageItem?.content?.text.let { text ->
                    assertThat(text).contains("Установите необходимую сумму обещанного платежа используя виртуальные кнопки.")
                    assertThat(text).contains("Сумма пополнения:")
                    assertThat(text).contains("600 ₽")
                }
                true
            }
            .verifyComplete()

        assertThat(dialogProcessor.contains(telegramId)).isTrue

        dialogProcessor.processOption(telegramId = telegramId, option = PromisePaymentAvailableOptions.AMOUNT_MINUS_ONE)
            .test()
            .expectNextMatches {
                val state = it.state!!
                assertThat(state.isFinished).isFalse
                assertThat(state.isCancelled).isFalse
                state.responseMessageItem?.content?.text.let { text ->
                    assertThat(text).contains("Установите необходимую сумму обещанного платежа используя виртуальные кнопки.")
                    assertThat(text).contains("Сумма пополнения:")
                    assertThat(text).contains("599 ₽")
                }
                true
            }
            .verifyComplete()

        assertThat(dialogProcessor.contains(telegramId)).isTrue

        dialogProcessor.processOption(telegramId = telegramId, option = PromisePaymentAvailableOptions.AMOUNT_SUBMIT)
            .test()
            .expectNextMatches {
                assertThat(it.user).isEqualTo(user)
                val state = it.state!!
                assertThat(state.isFinished).isTrue
                assertThat(state.isCancelled).isFalse

                val options = state.options as PromisePaymentOptions
                assertThat(options.amount).isEqualTo(599)

                assertThat(state.responseMessageItem).isNull()
                true
            }
            .verifyComplete()

        assertThat(dialogProcessor.contains(telegramId)).isFalse

        verify(billingWebClient).getRecommendedPayment(
            argThat {
                assertThat(mode).isEqualTo(1L)
                assertThat(agrmId).isEqualTo(999L)
                true
            }
        )
    }

    @Test
    fun testPromisePaymentInvalidStep() {
        val telegramId = 42L
        val user = UserEntity(telegramId = telegramId, agrmId = 999L)
        dialogProcessor.startDialog(user = user, command = Command.PROMISE_PAYMENT)
            .test()
            .expectNextMatches {
                val state = it.state!!
                assertThat(state.isFinished).isFalse
                assertThat(state.isCancelled).isFalse
                state.responseMessageItem?.content?.text.let { text ->
                    assertThat(text).contains("Предупреждение")
                    assertThat(text).contains("Обещанный платеж выдается сроком на 5 календарных дней")
                    assertThat(text).contains("Отвечая положительно, Вы соглашаетесь с условиями оказания услуги")
                }
                true
            }
            .verifyComplete()

        assertThat(dialogProcessor.contains(telegramId)).isTrue

        mockGetRecommendedPayment(
            GetRecommendedPaymentResponse(
                ret = BigDecimal("2000")
            )
        )
        dialogProcessor.processOption(telegramId = telegramId, option = PromisePaymentAvailableOptions.WARNING_APPROVE)
            .test()
            .expectNextMatches {
                val state = it.state!!
                assertThat(state.isFinished).isFalse
                assertThat(state.isCancelled).isTrue
                assertThat(state.responseMessageItem?.content?.text)
                    .contains("Задолженность на счете превышает максимальную сумму обещанного платежа. Функция недоступна.")
                true
            }
            .verifyComplete()

        assertThat(dialogProcessor.contains(telegramId)).isFalse

        verify(billingWebClient).getRecommendedPayment(
            argThat {
                assertThat(mode).isEqualTo(1L)
                assertThat(agrmId).isEqualTo(999L)
                true
            }
        )
    }

    @Test
    fun testPromisePaymentUpperBound() {
        val telegramId = 42L
        val user = UserEntity(telegramId = telegramId, agrmId = 999L)
        dialogProcessor.startDialog(user = user, command = Command.PROMISE_PAYMENT)
            .test()
            .expectNextMatches {
                val state = it.state!!
                assertThat(state.isFinished).isFalse
                assertThat(state.isCancelled).isFalse
                state.responseMessageItem?.content?.text.let { text ->
                    assertThat(text).contains("Предупреждение")
                    assertThat(text).contains("Обещанный платеж выдается сроком на 5 календарных дней")
                    assertThat(text).contains("Отвечая положительно, Вы соглашаетесь с условиями оказания услуги")
                }
                true
            }
            .verifyComplete()

        assertThat(dialogProcessor.contains(telegramId)).isTrue

        mockGetRecommendedPayment(
            GetRecommendedPaymentResponse(
                ret = BigDecimal("1450")
            )
        )
        dialogProcessor.processOption(telegramId = telegramId, option = PromisePaymentAvailableOptions.WARNING_APPROVE)
            .test()
            .expectNextMatches {
                val state = it.state!!
                assertThat(state.isFinished).isFalse
                assertThat(state.isCancelled).isFalse
                state.responseMessageItem?.content?.text.let { text ->
                    assertThat(text).contains("Установите необходимую сумму обещанного платежа используя виртуальные кнопки.")
                    assertThat(text).contains("Сумма пополнения:")
                    assertThat(text).contains("1450 ₽")
                }
                true
            }
            .verifyComplete()

        assertThat(dialogProcessor.contains(telegramId)).isTrue

        dialogProcessor.processOption(telegramId = telegramId, option = PromisePaymentAvailableOptions.AMOUNT_PLUS_ONE_HUNDRED)
            .test()
            .expectNextMatches {
                val state = it.state!!
                assertThat(state.isFinished).isFalse
                assertThat(state.isCancelled).isFalse
                state.responseMessageItem?.content?.text.let { text ->
                    assertThat(text).contains("Установите необходимую сумму обещанного платежа используя виртуальные кнопки.")
                    assertThat(text).contains("Сумма пополнения:")
                    assertThat(text).contains("1450 ₽")
                    assertThat(text).contains("Должна быть от 1 до 1500 ₽")
                }
                true
            }
            .verifyComplete()

        assertThat(dialogProcessor.contains(telegramId)).isTrue

        dialogProcessor.processOption(telegramId = telegramId, option = PromisePaymentAvailableOptions.AMOUNT_SUBMIT)
            .test()
            .expectNextMatches {
                assertThat(it.user).isEqualTo(user)
                val state = it.state!!
                assertThat(state.isFinished).isTrue
                assertThat(state.isCancelled).isFalse

                val options = state.options as PromisePaymentOptions
                assertThat(options.amount).isEqualTo(1450)

                assertThat(state.responseMessageItem).isNull()
                true
            }
            .verifyComplete()

        assertThat(dialogProcessor.contains(telegramId)).isFalse

        verify(billingWebClient).getRecommendedPayment(
            argThat {
                assertThat(mode).isEqualTo(1L)
                assertThat(agrmId).isEqualTo(999L)
                true
            }
        )
    }

    @Test
    fun testPromisePaymentLowBound() {
        val telegramId = 42L
        val user = UserEntity(telegramId = telegramId, agrmId = 999L)
        dialogProcessor.startDialog(user = user, command = Command.PROMISE_PAYMENT)
            .test()
            .expectNextMatches {
                val state = it.state!!
                assertThat(state.isFinished).isFalse
                assertThat(state.isCancelled).isFalse
                state.responseMessageItem?.content?.text.let { text ->
                    assertThat(text).contains("Предупреждение")
                    assertThat(text).contains("Обещанный платеж выдается сроком на 5 календарных дней")
                    assertThat(text).contains("Отвечая положительно, Вы соглашаетесь с условиями оказания услуги")
                }
                true
            }
            .verifyComplete()

        assertThat(dialogProcessor.contains(telegramId)).isTrue

        mockGetRecommendedPayment(
            GetRecommendedPaymentResponse(
                ret = BigDecimal("50")
            )
        )
        dialogProcessor.processOption(telegramId = telegramId, option = PromisePaymentAvailableOptions.WARNING_APPROVE)
            .test()
            .expectNextMatches {
                val state = it.state!!
                assertThat(state.isFinished).isFalse
                assertThat(state.isCancelled).isFalse
                state.responseMessageItem?.content?.text.let { text ->
                    assertThat(text).contains("Установите необходимую сумму обещанного платежа используя виртуальные кнопки.")
                    assertThat(text).contains("Сумма пополнения:")
                    assertThat(text).contains("50 ₽")
                }
                true
            }
            .verifyComplete()

        assertThat(dialogProcessor.contains(telegramId)).isTrue

        dialogProcessor.processOption(telegramId = telegramId, option = PromisePaymentAvailableOptions.AMOUNT_MINUS_ONE_HUNDRED)
            .test()
            .expectNextMatches {
                val state = it.state!!
                assertThat(state.isFinished).isFalse
                assertThat(state.isCancelled).isFalse
                state.responseMessageItem?.content?.text.let { text ->
                    assertThat(text).contains("Установите необходимую сумму обещанного платежа используя виртуальные кнопки.")
                    assertThat(text).contains("Сумма пополнения:")
                    assertThat(text).contains("50 ₽")
                    assertThat(text).contains("Должна быть от 1 до 1500 ₽")
                }
                true
            }
            .verifyComplete()

        assertThat(dialogProcessor.contains(telegramId)).isTrue

        dialogProcessor.processOption(telegramId = telegramId, option = PromisePaymentAvailableOptions.AMOUNT_SUBMIT)
            .test()
            .expectNextMatches {
                assertThat(it.user).isEqualTo(user)
                val state = it.state!!
                assertThat(state.isFinished).isTrue
                assertThat(state.isCancelled).isFalse

                val options = state.options as PromisePaymentOptions
                assertThat(options.amount).isEqualTo(50)

                assertThat(state.responseMessageItem).isNull()
                true
            }
            .verifyComplete()

        assertThat(dialogProcessor.contains(telegramId)).isFalse

        verify(billingWebClient).getRecommendedPayment(
            argThat {
                assertThat(mode).isEqualTo(1L)
                assertThat(agrmId).isEqualTo(999L)
                true
            }
        )
    }

    @Test
    fun testPromisePaymentRecommendedPaymentNull() {
        val telegramId = 42L
        val user = UserEntity(telegramId = telegramId, agrmId = 999L)
        dialogProcessor.startDialog(user = user, command = Command.PROMISE_PAYMENT)
            .test()
            .expectNextMatches {
                val state = it.state!!
                assertThat(state.isFinished).isFalse
                assertThat(state.isCancelled).isFalse
                state.responseMessageItem?.content?.text.let { text ->
                    assertThat(text).contains("Предупреждение")
                    assertThat(text).contains("Обещанный платеж выдается сроком на 5 календарных дней")
                    assertThat(text).contains("Отвечая положительно, Вы соглашаетесь с условиями оказания услуги")
                }
                true
            }
            .verifyComplete()

        assertThat(dialogProcessor.contains(telegramId)).isTrue

        mockGetRecommendedPayment(
            GetRecommendedPaymentResponse(
                ret = null
            )
        )
        dialogProcessor.processOption(telegramId = telegramId, option = PromisePaymentAvailableOptions.WARNING_APPROVE)
            .test()
            .expectErrorMatches {
                assertThat(it.message).contains("getRecommendedPayment payload is null")
                true
            }
            .verify()

        assertThat(dialogProcessor.contains(telegramId)).isFalse
    }

    @Test
    fun testNotificationTurnOn() {
        val telegramId = 42L
        val user = UserEntity(telegramId = telegramId, paymentNotificationEnabled = false)
        dialogProcessor.startDialog(user = user, command = Command.NOTIFICATION)
            .test()
            .expectNextMatches {
                val state = it.state!!
                assertThat(state.isFinished).isFalse
                assertThat(state.isCancelled).isFalse
                assertThat(state.responseMessageItem?.content?.text)
                    .contains(
                        "На данный момент у Вас",
                        "отключены",
                        "напоминания об оплате",
                        "Хотите",
                        "включить"
                    )
                true
            }
            .verifyComplete()

        assertThat(dialogProcessor.contains(telegramId)).isTrue

        dialogProcessor.processOption(telegramId = telegramId, option = NotificationAvailableOptions.TURN_ON)
            .test()
            .expectNextMatches {
                assertThat(it.user).isEqualTo(user)
                val state = it.state!!
                assertThat(state.isFinished).isTrue
                assertThat(state.isCancelled).isFalse

                val options = state.options as NotificationOptions
                assertThat(options.enable).isTrue

                assertThat(state.responseMessageItem).isNull()
                true
            }
            .verifyComplete()

        assertThat(dialogProcessor.contains(telegramId)).isFalse
    }

    @Test
    fun testNotificationTurnOff() {
        val telegramId = 42L
        val user = UserEntity(telegramId = telegramId, paymentNotificationEnabled = true)
        dialogProcessor.startDialog(user = user, command = Command.NOTIFICATION)
            .test()
            .expectNextMatches {
                val state = it.state!!
                assertThat(state.isFinished).isFalse
                assertThat(state.isCancelled).isFalse
                assertThat(state.responseMessageItem?.content?.text)
                    .contains(
                        "На данный момент у Вас",
                        "включены",
                        "напоминания об оплате",
                        "Хотите",
                        "отключить"
                    )
                true
            }
            .verifyComplete()

        assertThat(dialogProcessor.contains(telegramId)).isTrue

        dialogProcessor.processOption(telegramId = telegramId, option = NotificationAvailableOptions.TURN_OFF)
            .test()
            .expectNextMatches {
                assertThat(it.user).isEqualTo(user)
                val state = it.state!!
                assertThat(state.isFinished).isTrue
                assertThat(state.isCancelled).isFalse

                val options = state.options as NotificationOptions
                assertThat(options.enable).isFalse

                assertThat(state.responseMessageItem).isNull()
                true
            }
            .verifyComplete()

        assertThat(dialogProcessor.contains(telegramId)).isFalse
    }

    @Test
    fun testExitOk() {
        val telegramId = 42L
        val user = UserEntity(telegramId = telegramId)
        dialogProcessor.startDialog(user = user, command = Command.EXIT)
            .test()
            .expectNextMatches {
                val state = it.state!!
                assertThat(state.isFinished).isFalse
                assertThat(state.isCancelled).isFalse
                assertThat(state.responseMessageItem?.content?.text)
                    .contains("После выхода из системы функции бота станут недоступны. Вы уверены, что хотите отвязать устройство ?")
                true
            }
            .verifyComplete()

        assertThat(dialogProcessor.contains(telegramId)).isTrue

        dialogProcessor.processOption(telegramId = telegramId, option = ExitAvailableOptions.YES)
            .test()
            .expectNextMatches {
                assertThat(it.user).isEqualTo(user)
                val state = it.state!!
                assertThat(state.isFinished).isTrue
                assertThat(state.isCancelled).isFalse
                assertThat(state.options).isNull()
                assertThat(state.responseMessageItem).isNull()
                true
            }
            .verifyComplete()
    }

    private fun mockGetRecommendedPayment(response: GetRecommendedPaymentResponse) {
        whenever(
            billingWebClient.getRecommendedPayment(any())
        ).thenReturn(
            response.toMono()
        )
    }
}