package su.vshk.billing.bot

import com.nhaarman.mockitokotlin2.*
import org.assertj.core.api.Assertions.assertThat
import su.vshk.billing.bot.dao.model.UserEntity
import su.vshk.billing.bot.dao.repository.UserRepository
import su.vshk.billing.bot.web.client.BillingWebClient
import su.vshk.billing.bot.web.dto.BillingResponseItem
import su.vshk.billing.bot.web.dto.client.ClientPromisePaymentResponse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.Chat
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import su.vshk.billing.bot.config.BotProperties
import su.vshk.billing.bot.dao.model.Command
import su.vshk.billing.bot.dao.model.GenericCommand
import su.vshk.billing.bot.dao.model.PaymentNotificationMessageEntity
import su.vshk.billing.bot.dao.repository.PaymentNotificationMessageRepository
import su.vshk.billing.bot.dialog.option.*
import su.vshk.billing.bot.message.dto.ResponseMessageItem
import su.vshk.billing.bot.service.LoginMessageService
import su.vshk.billing.bot.web.dto.manager.*
import java.math.BigDecimal
import java.util.*

@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class BotTest {

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var paymentNotificationMessageRepository: PaymentNotificationMessageRepository

    @Autowired
    private lateinit var loginMessageService: LoginMessageService

    @Autowired
    private lateinit var properties: BotProperties

    @SpyBean
    private lateinit var bot: Bot

    @MockBean
    private lateinit var billingWebClient: BillingWebClient

    private val telegramId = 42L
    private val buttonRequestMessageId = 1

    @BeforeEach
    fun setUp() {
        userRepository.deleteAll()
        paymentNotificationMessageRepository.deleteAll()
    }

    @ParameterizedTest
    @ValueSource(strings = ["/start", "some_text"])
    fun testSuccessfulUserLogin(command: String) {
        val sendMessageChatIdCaptor = argumentCaptor<Long>()
        val sendMessageResponseContentCaptor = argumentCaptor<ResponseMessageItem.Content>()

        doReturn(Message().also { it.messageId = 11 }.toMono())
            .doReturn(Message().also { it.messageId = 21 }.toMono())
            .doReturn(Message().also { it.messageId = 31 }.toMono())
            .whenever(bot).sendMessage(sendMessageChatIdCaptor.capture(), sendMessageResponseContentCaptor.capture())

        val deleteMessageChatIdCaptor = argumentCaptor<Long>()
        val deleteMessageIdsCaptor = argumentCaptor<List<Int>>()

        doReturn(Mono.empty<Unit>())
            .whenever(bot).deleteMessages(deleteMessageChatIdCaptor.capture(), deleteMessageIdsCaptor.capture())

        val uid = 13L
        mockGetClientId(uid)

        val agrmId = 999L
        mockGetVgroups(agrmId = agrmId)

        sendText(data = command, requestMessageId = 10)
        sendText(data = "user", requestMessageId = 20)
        sendText(data = "1234", requestMessageId = 30)

        assertThat(sendMessageChatIdCaptor.allValues).containsExactly(telegramId, telegramId, telegramId)
        sendMessageResponseContentCaptor.allValues.map { it.text }.let { messages ->
            assertThat(messages.size).isEqualTo(3)
            assertThat(messages[0]).contains("Добро пожаловать!", "Введите логин:")
            assertThat(messages[1]).contains("Введите пароль:")
            assertThat(messages[2]).contains("Главное меню")
        }

        assertThat(deleteMessageChatIdCaptor.firstValue).isEqualTo(telegramId)
        assertThat(deleteMessageIdsCaptor.firstValue).containsExactly(30, 21, 20, 11, 10)

        verify(billingWebClient).getClientId(
            argThat {
                assertThat(this.login).isEqualTo("user")
                assertThat(this.password).isEqualTo("1234")
                true
            }
        )

        verify(billingWebClient).getVgroups(
            argThat {
                assertThat(this.flt?.userId).isEqualTo(uid)
                true
            }
        )

        val user = userRepository.findAll().let {
            assertThat(it.size).isEqualTo(1)
            it[0]
        }
        assertThat(user.telegramId).isEqualTo(telegramId)
        assertThat(user.uid).isEqualTo(uid)
        assertThat(user.login).isEqualTo("user")
        assertThat(user.password).isEqualTo("1234")
        assertThat(user.agrmId).isEqualTo(agrmId)

        assertThat(paymentNotificationMessageRepository.findAll()).isEmpty()

        assertThat(loginMessageService.isEmpty(user.telegramId)).isTrue
    }

    @Test
    fun testUnsuccessfulUserLogin() {
        val sendMessageChatIdCaptor = argumentCaptor<Long>()
        val sendMessageResponseContentCaptor = argumentCaptor<ResponseMessageItem.Content>()

        doReturn(Message().also { it.messageId = 11 }.toMono())
            .doReturn(Message().also { it.messageId = 21 }.toMono())
            .doReturn(Message().also { it.messageId = 31 }.toMono())
            .whenever(bot).sendMessage(sendMessageChatIdCaptor.capture(), sendMessageResponseContentCaptor.capture())

        val deleteMessageChatIdCaptor = argumentCaptor<Long>()
        val deleteMessageIdsCaptor = argumentCaptor<List<Int>>()

        doReturn(Mono.empty<Unit>())
            .whenever(bot).deleteMessages(deleteMessageChatIdCaptor.capture(), deleteMessageIdsCaptor.capture())

        mockGetClientId(null)

        sendText(data = Command.START.value, requestMessageId = 10)
        sendText(data = "user", requestMessageId = 20)
        sendText(data = "1234", requestMessageId = 30)

        assertThat(sendMessageChatIdCaptor.allValues).containsExactly(telegramId, telegramId, telegramId)
        sendMessageResponseContentCaptor.allValues.map { it.text }.let { messages ->
            assertThat(messages.size).isEqualTo(3)
            assertThat(messages[0]).contains("Добро пожаловать!", "Введите логин:")
            assertThat(messages[1]).contains("Введите пароль:")
            assertThat(messages[2]).contains("Неправильные логин/пароль. Попробуйте снова")
        }

        assertThat(deleteMessageChatIdCaptor.firstValue).isEqualTo(telegramId)
        assertThat(deleteMessageIdsCaptor.firstValue).containsExactly(30, 21, 20, 11, 10)

        verify(billingWebClient).getClientId(
            argThat {
                assertThat(this.login).isEqualTo("user")
                assertThat(this.password).isEqualTo("1234")
                true
            }
        )
        assertThat(userRepository.findAll()).isEmpty()
        assertThat(paymentNotificationMessageRepository.findAll()).isEmpty()
        assertThat(loginMessageService.isEmpty(telegramId)).isTrue
    }

    @Test
    fun testRemoveMessageIfRegisteredUserSendText() {
        preSaveUser()

        val deleteMessageChatIdCaptor = argumentCaptor<Long>()
        val deleteMessageIdsCaptor = argumentCaptor<List<Int>>()

        doReturn(Mono.empty<Unit>())
            .whenever(bot).deleteMessages(deleteMessageChatIdCaptor.capture(), deleteMessageIdsCaptor.capture())

        sendText(data = "some_text", requestMessageId = 10)

        assertThat(deleteMessageChatIdCaptor.firstValue).isEqualTo(telegramId)
        assertThat(deleteMessageIdsCaptor.firstValue).containsExactly(10)
    }

    @Test
    fun testSendMenuIfRegisteredUserSendStartAsText() {
        preSaveUser()

        val sendMessageChatIdCaptor = argumentCaptor<Long>()
        val sendMessageResponseContentCaptor = argumentCaptor<ResponseMessageItem.Content>()

        doReturn(Message().also { it.messageId = 11 }.toMono())
            .whenever(bot).sendMessage(sendMessageChatIdCaptor.capture(), sendMessageResponseContentCaptor.capture())

        val deleteMessageChatIdCaptor = argumentCaptor<Long>()
        val deleteMessageIdsCaptor = argumentCaptor<List<Int>>()

        doReturn(Mono.empty<Unit>())
            .whenever(bot).deleteMessages(deleteMessageChatIdCaptor.capture(), deleteMessageIdsCaptor.capture())

        sendText(data = Command.START.value, requestMessageId = 10)

        assertThat(sendMessageChatIdCaptor.allValues).containsExactly(telegramId)
        sendMessageResponseContentCaptor.allValues.map { it.text }.let { messages ->
            assertThat(messages.size).isEqualTo(1)
            assertThat(messages[0]).contains("Главное меню")
        }

        assertThat(deleteMessageChatIdCaptor.firstValue).isEqualTo(telegramId)
        assertThat(deleteMessageIdsCaptor.firstValue).containsExactly(10)
    }

    @Test
    fun testErrorOccursWhenUserTryToRegister() {
        val sendMessageChatIdCaptor = argumentCaptor<Long>()
        val sendMessageResponseContentCaptor = argumentCaptor<ResponseMessageItem.Content>()

        doReturn(Message().also { it.messageId = 11 }.toMono())
            .doReturn(Message().also { it.messageId = 21 }.toMono())
            .doReturn(Message().also { it.messageId = 0 }.toMono())
            .doReturn(Message().also { it.messageId = 31 }.toMono())
            .whenever(bot).sendMessage(sendMessageChatIdCaptor.capture(), sendMessageResponseContentCaptor.capture())

        val deleteMessageChatIdCaptor = argumentCaptor<Long>()
        val deleteMessageIdsCaptor = argumentCaptor<List<Int>>()

        doReturn(Mono.empty<Unit>())
            .whenever(bot).deleteMessages(deleteMessageChatIdCaptor.capture(), deleteMessageIdsCaptor.capture())

        mockGetClientIdError()

        sendText(data = Command.START.value, requestMessageId = 10)
        sendText(data = "user", requestMessageId = 20)
        sendText(data = "1234", requestMessageId = 30)

        assertThat(sendMessageChatIdCaptor.allValues).containsExactly(telegramId, telegramId, properties.errorGroupNotification.chatId, telegramId)
        sendMessageResponseContentCaptor.allValues.map { it.text }.let { messages ->
            assertThat(messages.size).isEqualTo(4)
            assertThat(messages[0]).contains("Добро пожаловать!", "Введите логин:")
            assertThat(messages[1]).contains("Введите пароль:")
            assertThat(messages[2]).contains("Код ошибки:")
            assertThat(messages[3]).contains("К сожалению, на данный момент функция недоступна")
        }

        assertThat(deleteMessageChatIdCaptor.firstValue).isEqualTo(telegramId)
        assertThat(deleteMessageIdsCaptor.firstValue).containsExactly(30, 21, 20, 11, 10)

        assertThat(userRepository.findAll()).isEmpty()
        assertThat(paymentNotificationMessageRepository.findAll()).isEmpty()
        assertThat(loginMessageService.isEmpty(telegramId)).isTrue
    }

    @Test
    fun testErrorOccursWhenUserUseMenu() {
        val sendMessageChatIdCaptor = argumentCaptor<Long>()
        val sendMessageResponseContentCaptor = argumentCaptor<ResponseMessageItem.Content>()

        doReturn(Message().also { it.messageId = 10 }.toMono())
            .whenever(bot).sendMessage(sendMessageChatIdCaptor.capture(), sendMessageResponseContentCaptor.capture())

        val editMessageChatIdCaptor = argumentCaptor<Long>()
        val editMessageMessageIdCaptor = argumentCaptor<Int>()
        val editMessageResponseContentCaptor = argumentCaptor<ResponseMessageItem.Content>()

        doReturn(Mono.empty<Unit>())
            .whenever(bot).editMessage(editMessageChatIdCaptor.capture(), editMessageMessageIdCaptor.capture(), editMessageResponseContentCaptor.capture())

        preSaveUser()
        mockGetVgroupsError()

        pushButton(Command.INFO.value)

        assertThat(sendMessageChatIdCaptor.firstValue).isEqualTo(properties.errorGroupNotification.chatId)
        assertThat(sendMessageResponseContentCaptor.firstValue.text).contains("Код ошибки:")

        assertThat(editMessageChatIdCaptor.firstValue).isEqualTo(telegramId)
        assertThat(editMessageMessageIdCaptor.firstValue).isEqualTo(buttonRequestMessageId)
        assertThat(editMessageResponseContentCaptor.firstValue.text).contains("К сожалению, на данный момент функция недоступна")
    }

    @Test
    fun testUnexpectedCommandInput() {
        // Может произойти, если бот перезагрузился, а пользователь был в диалоге.
        // Тогда пользователь введет опцию с кнопки, а бот ожидает ввода команды.

        val editMessageChatIdCaptor = argumentCaptor<Long>()
        val editMessageMessageIdCaptor = argumentCaptor<Int>()
        val editMessageResponseContentCaptor = argumentCaptor<ResponseMessageItem.Content>()

        doReturn(Mono.empty<Unit>())
            .whenever(bot).editMessage(editMessageChatIdCaptor.capture(), editMessageMessageIdCaptor.capture(), editMessageResponseContentCaptor.capture())

        preSaveUser()

        pushButton(PromisePaymentAvailableOptions.AMOUNT_MINUS_ONE)

        assertThat(editMessageChatIdCaptor.firstValue).isEqualTo(telegramId)
        assertThat(editMessageMessageIdCaptor.firstValue).isEqualTo(buttonRequestMessageId)
        assertThat(editMessageResponseContentCaptor.firstValue.text).contains("Главное меню")
    }

    @Test
    fun testGoBackToMenu() {
        val editMessageChatIdCaptor = argumentCaptor<Long>()
        val editMessageMessageIdCaptor = argumentCaptor<Int>()
        val editMessageResponseContentCaptor = argumentCaptor<ResponseMessageItem.Content>()

        doReturn(Mono.empty<Unit>())
            .whenever(bot).editMessage(editMessageChatIdCaptor.capture(), editMessageMessageIdCaptor.capture(), editMessageResponseContentCaptor.capture())

        preSaveUser()

        pushButton(Command.MENU.value)

        assertThat(editMessageChatIdCaptor.firstValue).isEqualTo(telegramId)
        assertThat(editMessageMessageIdCaptor.firstValue).isEqualTo(buttonRequestMessageId)
        assertThat(editMessageResponseContentCaptor.firstValue.text).contains("Главное меню")
    }

    @Test
    fun testSendTextWhenUserIsInDialog() {
        val editMessageChatIdCaptor = argumentCaptor<Long>()
        val editMessageMessageIdCaptor = argumentCaptor<Int>()
        val editMessageResponseContentCaptor = argumentCaptor<ResponseMessageItem.Content>()

        doReturn(Mono.empty<Unit>())
            .doReturn(Mono.empty<Unit>())
            .whenever(bot).editMessage(editMessageChatIdCaptor.capture(), editMessageMessageIdCaptor.capture(), editMessageResponseContentCaptor.capture())

        val deleteMessageChatIdCaptor = argumentCaptor<Long>()
        val deleteMessageIdsCaptor = argumentCaptor<List<Int>>()

        doReturn(Mono.empty<Unit>())
            .whenever(bot).deleteMessages(deleteMessageChatIdCaptor.capture(), deleteMessageIdsCaptor.capture())

        preSaveUser()
        pushButton(Command.EXIT.value)
        sendText(data = "some_text", requestMessageId = 10)
        pushButton(ExitAvailableOptions.YES)

        assertThat(editMessageChatIdCaptor.allValues).containsExactly(telegramId, telegramId)
        assertThat(editMessageMessageIdCaptor.allValues).containsExactly(buttonRequestMessageId, buttonRequestMessageId)
        editMessageResponseContentCaptor.allValues.map { it.text }.let { messages ->
            assertThat(messages.size).isEqualTo(2)
            assertThat(messages[0]).contains("После выхода из системы функции бота станут недоступны. Вы уверены, что хотите отвязать устройство ?")
            assertThat(messages[1]).contains("Вы успешно вышли.")
        }

        assertThat(deleteMessageChatIdCaptor.firstValue).isEqualTo(telegramId)
        assertThat(deleteMessageIdsCaptor.firstValue).containsExactly(10)

        assertThat(userRepository.findAll()).isEmpty()
        assertThat(paymentNotificationMessageRepository.findAll()).isEmpty()
    }

    /**
     * Пользователь успешно выходит из бота.
     * При этом у него в чате есть напоминание об оплате, которое тоже будет удалено.
     */
    @Test
    fun testExitAndDeletePaymentNotification() {
        val editMessageChatIdCaptor = argumentCaptor<Long>()
        val editMessageMessageIdCaptor = argumentCaptor<Int>()
        val editMessageResponseContentCaptor = argumentCaptor<ResponseMessageItem.Content>()

        doReturn(Mono.empty<Unit>())
            .doReturn(Mono.empty<Unit>())
            .whenever(bot).editMessage(editMessageChatIdCaptor.capture(), editMessageMessageIdCaptor.capture(), editMessageResponseContentCaptor.capture())

        val deleteMessageChatIdCaptor = argumentCaptor<Long>()
        val deleteMessageIdsCaptor = argumentCaptor<List<Int>>()

        doReturn(Mono.empty<Unit>())
            .whenever(bot).deleteMessages(deleteMessageChatIdCaptor.capture(), deleteMessageIdsCaptor.capture())

        preSaveUser()

        val paymentNotificationMessageId = 100
        preSavePaymentNotificationMessage(paymentNotificationMessageId)

        pushButton(Command.EXIT.value)
        pushButton(ExitAvailableOptions.YES)

        assertThat(editMessageChatIdCaptor.allValues).containsExactly(telegramId, telegramId)
        assertThat(editMessageMessageIdCaptor.allValues).containsExactly(buttonRequestMessageId, buttonRequestMessageId)
        editMessageResponseContentCaptor.allValues.map { it.text }.let { messages ->
            assertThat(messages.size).isEqualTo(2)
            assertThat(messages[0]).contains("После выхода из системы функции бота станут недоступны. Вы уверены, что хотите отвязать устройство ?")
            assertThat(messages[1]).contains("Вы успешно вышли.")
        }

        assertThat(deleteMessageChatIdCaptor.firstValue).isEqualTo(telegramId)
        assertThat(deleteMessageIdsCaptor.firstValue).containsExactly(paymentNotificationMessageId)

        assertThat(userRepository.findAll()).isEmpty()
        assertThat(paymentNotificationMessageRepository.findAll()).isEmpty()
    }

    @Test
    fun testInfo() {
        val editMessageChatIdCaptor = argumentCaptor<Long>()
        val editMessageMessageIdCaptor = argumentCaptor<Int>()
        val editMessageResponseContentCaptor = argumentCaptor<ResponseMessageItem.Content>()

        doReturn(Mono.empty<Unit>())
            .whenever(bot).editMessage(editMessageChatIdCaptor.capture(), editMessageMessageIdCaptor.capture(), editMessageResponseContentCaptor.capture())

        preSaveUser()
        mockGetVgroups(username = "Иванов И.И", agrmnum = "1917-000", balance = BigDecimal("3000.23"))

        val recommendedPaymentCaptor = mockGetRecommendedPayment(
            recommendedPayment = BigDecimal("300"),
            recommendedPaymentAndBalanceDiff = BigDecimal("500")
        )

        mockGetAccount(
            promiseCredit = BigDecimal("1500.15"),
            email = "foobar@mail.ru"
        )

        pushButton(Command.INFO.value)

        assertThat(editMessageChatIdCaptor.firstValue).isEqualTo(telegramId)
        assertThat(editMessageMessageIdCaptor.firstValue).isEqualTo(buttonRequestMessageId)
        assertThat(
            editMessageResponseContentCaptor.firstValue.text
        ).contains(
            "ФИО",
            "Иванов И.И",
            "Номер договора",
            "1917-00",
            "Баланс",
            "3 000.23 ₽",
            "Ежемесячный платеж",
            "300 ₽",
            "Рекомендуется внести",
            "500 ₽",
            "Подключен обещанный платеж",
            "1 500.15 ₽",
            "E-mail",
            "foobar@mail.ru"
        )

        verify(billingWebClient).getVgroups(
            argThat {
                assertThat(this.flt?.userId).isEqualTo(13L)
                true
            }
        )

        recommendedPaymentCaptor.allValues.let { requests ->
            assertThat(requests.size).isEqualTo(2)
            assertThat(requests[0].agrmId).isEqualTo(999L)
            assertThat(requests[0].mode).isEqualTo(0L)

            assertThat(requests[1].agrmId).isEqualTo(999L)
            assertThat(requests[1].mode).isEqualTo(1L)
        }

        verify(billingWebClient).getAccount(
            argThat {
                assertThat(this.uid).isEqualTo(13L)
                true
            }
        )
    }

    @Test
    fun testExit() {
        val editMessageChatIdCaptor = argumentCaptor<Long>()
        val editMessageMessageIdCaptor = argumentCaptor<Int>()
        val editMessageResponseContentCaptor = argumentCaptor<ResponseMessageItem.Content>()

        doReturn(Mono.empty<Unit>())
            .doReturn(Mono.empty<Unit>())
            .whenever(bot).editMessage(editMessageChatIdCaptor.capture(), editMessageMessageIdCaptor.capture(), editMessageResponseContentCaptor.capture())

        preSaveUser()

        pushButton(Command.EXIT.value)
        pushButton(ExitAvailableOptions.YES)

        assertThat(editMessageChatIdCaptor.allValues).containsExactly(telegramId, telegramId)
        assertThat(editMessageMessageIdCaptor.allValues).containsExactly(buttonRequestMessageId, buttonRequestMessageId)
        editMessageResponseContentCaptor.allValues.map { it.text }.let { messages ->
            assertThat(messages.size).isEqualTo(2)
            assertThat(messages[0]).contains("После выхода из системы функции бота станут недоступны. Вы уверены, что хотите отвязать устройство ?")
            assertThat(messages[1]).contains("Вы успешно вышли.")
        }

        assertThat(userRepository.findAll()).isEmpty()
        assertThat(paymentNotificationMessageRepository.findAll()).isEmpty()
    }

    @Test
    fun testPayments() {
        val editMessageChatIdCaptor = argumentCaptor<Long>()
        val editMessageMessageIdCaptor = argumentCaptor<Int>()
        val editMessageResponseContentCaptor = argumentCaptor<ResponseMessageItem.Content>()

        doReturn(Mono.empty<Unit>())
            .doReturn(Mono.empty<Unit>())
            .whenever(bot).editMessage(editMessageChatIdCaptor.capture(), editMessageMessageIdCaptor.capture(), editMessageResponseContentCaptor.capture())

        preSaveUser()
        mockGetPaymentsEmpty()

        pushButton(Command.PAYMENTS.value)
        pushButton(PaymentsAvailableOptions.PERIOD_ONE_MONTH)

        assertThat(editMessageChatIdCaptor.allValues).containsExactly(telegramId, telegramId)
        assertThat(editMessageMessageIdCaptor.allValues).containsExactly(buttonRequestMessageId, buttonRequestMessageId)
        editMessageResponseContentCaptor.allValues.map { it.text }.let { messages ->
            assertThat(messages.size).isEqualTo(2)
            assertThat(messages[0]).contains("За какой период времени Вы хотите увидеть список платежей ?")
            assertThat(messages[1]).contains("Платежи с", "Нет данных.")
        }

        verify(billingWebClient).getPayments(
            argThat {
                val flt = this.flt!!
                assertThat(flt.agrmId).isEqualTo(999L)
                assertThat(flt.dateFrom).isNotNull
                assertThat(flt.dateTo).isNotNull
                true
            }
        )
    }

    @Test
    fun testClientPromisePayment() {
        val editMessageChatIdCaptor = argumentCaptor<Long>()
        val editMessageMessageIdCaptor = argumentCaptor<Int>()
        val editMessageResponseContentCaptor = argumentCaptor<ResponseMessageItem.Content>()

        doReturn(Mono.empty<Unit>())
            .doReturn(Mono.empty<Unit>())
            .doReturn(Mono.empty<Unit>())
            .whenever(bot).editMessage(editMessageChatIdCaptor.capture(), editMessageMessageIdCaptor.capture(), editMessageResponseContentCaptor.capture())

        val user = preSaveUser()
        mockGetRecommendedPayment(BigDecimal("1000"))
        mockClientPromisePaymentSuccess()

        pushButton(Command.PROMISE_PAYMENT.value)
        pushButton(PromisePaymentAvailableOptions.WARNING_APPROVE)
        pushButton(PromisePaymentAvailableOptions.AMOUNT_SUBMIT)

        assertThat(editMessageChatIdCaptor.allValues).containsExactly(telegramId, telegramId, telegramId)
        assertThat(editMessageMessageIdCaptor.allValues).containsExactly(buttonRequestMessageId, buttonRequestMessageId, buttonRequestMessageId)
        editMessageResponseContentCaptor.allValues.map { it.text }.let { messages ->
            assertThat(messages.size).isEqualTo(3)
            assertThat(messages[0]).contains("Обещанный платеж выдается сроком на 5 календарных дней")
            assertThat(messages[1]).contains("Установите необходимую сумму обещанного платежа используя виртуальные кнопки.", "Сумма пополнения:", "1000 ₽")
            assertThat(messages[2]).contains("Обещанный платеж успешно подключен")
        }

        verify(billingWebClient).getRecommendedPayment(
            argThat {
                assertThat(mode).isEqualTo(1L)
                assertThat(agrmId).isEqualTo(999L)
                true
            }
        )

        verify(billingWebClient).clientPromisePayment(
            argThat {
                assertThat(this.telegramId).isEqualTo(user.telegramId)
                assertThat(this.uid).isEqualTo(user.uid)
                assertThat(this.login).isEqualTo(user.login)
                assertThat(this.password).isEqualTo(user.password)
                assertThat(this.agrmId).isEqualTo(user.agrmId)
                true
            },
            argThat {
                assertThat(this.agrmId).isEqualTo(999L)
                assertThat(this.amount).isEqualTo(1000)
                true
            }
        )
    }

    @Test
    fun testTariffs() {
        val editMessageChatIdCaptor = argumentCaptor<Long>()
        val editMessageMessageIdCaptor = argumentCaptor<Int>()
        val editMessageResponseContentCaptor = argumentCaptor<ResponseMessageItem.Content>()

        doReturn(Mono.empty<Unit>())
            .whenever(bot).editMessage(editMessageChatIdCaptor.capture(), editMessageMessageIdCaptor.capture(), editMessageResponseContentCaptor.capture())

        preSaveUser()
        mockGetVgroups(
            tariffs = listOf(
                "Услуги",
                "ТВ - Смотрешка \"Пакет Эксклюзив\"",
                "ФЛ - МКД - Начальный (MX)",
                "ТВ - 24 часа ТВ  (Отключен)"
            )
        )

        pushButton(Command.TARIFFS.value)

        assertThat(editMessageChatIdCaptor.firstValue).isEqualTo(telegramId)
        assertThat(editMessageMessageIdCaptor.firstValue).isEqualTo(buttonRequestMessageId)
        editMessageResponseContentCaptor.firstValue.text.let { text ->
            assertThat(text).contains("Смотрешка \"Пакет Эксклюзив\"", "Начальный")
            assertThat(text).doesNotContain("Услуги", "ТВ - 24 часа ТВ  (Отключен)")
        }
    }

    @Test
    fun testTurnOnNotification() {
        val editMessageChatIdCaptor = argumentCaptor<Long>()
        val editMessageMessageIdCaptor = argumentCaptor<Int>()
        val editMessageResponseContentCaptor = argumentCaptor<ResponseMessageItem.Content>()

        doReturn(Mono.empty<Unit>())
            .doReturn(Mono.empty<Unit>())
            .whenever(bot).editMessage(editMessageChatIdCaptor.capture(), editMessageMessageIdCaptor.capture(), editMessageResponseContentCaptor.capture())

        preSaveUser()

        pushButton(Command.NOTIFICATION.value)
        pushButton(NotificationAvailableOptions.TURN_ON)

        assertThat(editMessageChatIdCaptor.allValues).containsExactly(telegramId, telegramId)
        assertThat(editMessageMessageIdCaptor.allValues).containsExactly(buttonRequestMessageId, buttonRequestMessageId)
        editMessageResponseContentCaptor.allValues.map { it.text }.let { messages ->
            assertThat(messages.size).isEqualTo(2)
            assertThat(
                messages[0]
            ).contains(
                "На данный момент у Вас",
                "отключены",
                "напоминания об оплате",
                "Хотите",
                "включить"
            )
            assertThat(messages[1]).contains("Напоминания об оплате включены")
        }

        assertThat(
            userRepository.findById(telegramId).get().paymentNotificationEnabled
        ).isTrue
    }

    @Test
    fun testContacts() {
        val editMessageChatIdCaptor = argumentCaptor<Long>()
        val editMessageMessageIdCaptor = argumentCaptor<Int>()
        val editMessageResponseContentCaptor = argumentCaptor<ResponseMessageItem.Content>()

        doReturn(Mono.empty<Unit>())
            .whenever(bot).editMessage(editMessageChatIdCaptor.capture(), editMessageMessageIdCaptor.capture(), editMessageResponseContentCaptor.capture())

        preSaveUser()

        pushButton(Command.CONTACTS.value)

        assertThat(editMessageChatIdCaptor.firstValue).isEqualTo(telegramId)
        assertThat(editMessageMessageIdCaptor.firstValue).isEqualTo(buttonRequestMessageId)
        assertThat(editMessageResponseContentCaptor.firstValue.text).contains("Техническая поддержка", "Клиентский офис")
    }

    @Test
    fun testDeletePaymentNotification() {
        val deleteMessageChatIdCaptor = argumentCaptor<Long>()
        val deleteMessageIdsCaptor = argumentCaptor<List<Int>>()

        doReturn(Mono.empty<Unit>())
            .whenever(bot).deleteMessages(deleteMessageChatIdCaptor.capture(), deleteMessageIdsCaptor.capture())

        preSaveUser()
        preSavePaymentNotificationMessage(buttonRequestMessageId)

        pushButton(GenericCommand.DELETE_PAYMENT_NOTIFICATION)

        assertThat(deleteMessageChatIdCaptor.firstValue).isEqualTo(telegramId)
        assertThat(deleteMessageIdsCaptor.firstValue).containsExactly(buttonRequestMessageId)

        assertThat(paymentNotificationMessageRepository.findAll()).isEmpty()
    }

    @Test
    fun testDeletePaymentNotificationWhenUserIsInDialogForAnotherCommand() {
        val deleteMessageChatIdCaptor = argumentCaptor<Long>()
        val deleteMessageIdsCaptor = argumentCaptor<List<Int>>()

        doReturn(Mono.empty<Unit>())
            .whenever(bot).deleteMessages(deleteMessageChatIdCaptor.capture(), deleteMessageIdsCaptor.capture())

        val editMessageChatIdCaptor = argumentCaptor<Long>()
        val editMessageMessageIdCaptor = argumentCaptor<Int>()
        val editMessageResponseContentCaptor = argumentCaptor<ResponseMessageItem.Content>()

        doReturn(Mono.empty<Unit>())
            .doReturn(Mono.empty<Unit>())
            .whenever(bot).editMessage(editMessageChatIdCaptor.capture(), editMessageMessageIdCaptor.capture(), editMessageResponseContentCaptor.capture())

        preSaveUser()
        preSavePaymentNotificationMessage(buttonRequestMessageId)
        mockGetPaymentsEmpty()

        pushButton(Command.PAYMENTS.value)
        pushButton(GenericCommand.DELETE_PAYMENT_NOTIFICATION)
        pushButton(PaymentsAvailableOptions.PERIOD_ONE_MONTH)

        assertThat(deleteMessageChatIdCaptor.firstValue).isEqualTo(telegramId)
        assertThat(deleteMessageIdsCaptor.firstValue).containsExactly(buttonRequestMessageId)

        assertThat(editMessageChatIdCaptor.allValues).containsExactly(telegramId, telegramId)
        assertThat(editMessageMessageIdCaptor.allValues).containsExactly(buttonRequestMessageId, buttonRequestMessageId)
        editMessageResponseContentCaptor.allValues.map { it.text }.let { messages ->
            assertThat(messages.size).isEqualTo(2)
            assertThat(messages[0]).contains("За какой период времени Вы хотите увидеть список платежей ?")
            assertThat(messages[1]).contains("Платежи с", "Нет данных.")
        }

        verify(billingWebClient).getPayments(
            argThat {
                val flt = this.flt!!
                assertThat(flt.agrmId).isEqualTo(999L)
                assertThat(flt.dateFrom).isNotNull
                assertThat(flt.dateTo).isNotNull
                true
            }
        )

        assertThat(paymentNotificationMessageRepository.findAll()).isEmpty()
    }

    private fun preSaveUser(): UserEntity =
        UserEntity(
            telegramId = telegramId,
            uid = 13L,
            login = "user",
            password = "1234",
            agrmId = 999L,
            paymentNotificationEnabled = false
        ).let { userRepository.save(it) }

    private fun preSavePaymentNotificationMessage(paymentNotificationMessageId: Int = 0) =
        PaymentNotificationMessageEntity(
            telegramId = telegramId,
            messageId = paymentNotificationMessageId
        ).let { paymentNotificationMessageRepository.save(it) }

    private fun sendText(data: String, requestMessageId: Int) {
        val update = createTextUpdate(chatId = telegramId, text = data, requestMessageId = requestMessageId)
        bot.onUpdateReceived(update)
        Thread.sleep(1000L)
    }

    private fun pushButton(callbackData: String) {
        val update = createButtonUpdate(chatId = telegramId, callbackData = callbackData)
        bot.onUpdateReceived(update)
        Thread.sleep(1000L)
    }

    private fun createTextUpdate(chatId: Long, text: String, requestMessageId: Int): Update {
        val chat = Chat()
            .also { it.id = chatId }

        val msg = Message()
            .also {
                it.text = text
                it.chat = chat
                it.messageId = requestMessageId
            }

        return Update()
            .also { it.message = msg }
    }

    private fun createButtonUpdate(chatId: Long, callbackData: String): Update {
        val chat = Chat()
            .also { it.id = chatId }

        val msg = Message()
            .also {
                it.chat = chat
                it.messageId = buttonRequestMessageId
            }

        val callbackQuery = CallbackQuery()
            .also {
                it.message = msg
                it.data = callbackData
            }

        return Update()
            .also { it.callbackQuery = callbackQuery }
    }

    private fun mockGetClientId(uid: Long?) {
        whenever(
            billingWebClient.getClientId(any())
        ).thenReturn(
            Optional.ofNullable(uid).toMono()
        )
    }

    private fun mockGetClientIdError() {
        whenever(
            billingWebClient.getClientId(any())
        ).thenThrow(
            RuntimeException("some error occurs")
        )
    }

    private fun mockGetVgroups(
        username: String? = null,
        agrmnum: String? = null,
        agrmId: Long? = null,
        balance: BigDecimal? = null
    ) {
        whenever(
            billingWebClient.getVgroups(any())
        ).thenReturn(
            GetVgroupsResponse(
                ret = listOf(
                    GetVgroupsRet(
                        username = username,
                        agrmNum = agrmnum,
                        agrmId = agrmId,
                        balance = balance
                    )
                )
            ).toMono()
        )
    }

    private fun mockGetVgroups(tariffs: List<String>) {
        whenever(
            billingWebClient.getVgroups(any())
        ).thenReturn(
            tariffs
                .map { GetVgroupsRet(tariffDescription = it) }
                .let { GetVgroupsResponse(ret = it).toMono() }
        )
    }

    private fun mockGetVgroupsError() {
        whenever(
            billingWebClient.getVgroups(any())
        ).thenReturn(
            RuntimeException("error occurs").toMono()
        )
    }

    private fun mockGetRecommendedPayment(recommendedPayment: BigDecimal) {
        whenever(
            billingWebClient.getRecommendedPayment(any())
        ).thenReturn(
            GetRecommendedPaymentResponse(ret = recommendedPayment).toMono()
        )
    }

    private fun mockGetRecommendedPayment(
        recommendedPayment: BigDecimal,
        recommendedPaymentAndBalanceDiff: BigDecimal
    ): KArgumentCaptor<GetRecommendedPaymentRequest> {
        val captor = argumentCaptor<GetRecommendedPaymentRequest>()
        whenever(
            billingWebClient.getRecommendedPayment(captor.capture())
        ).thenReturn(
            GetRecommendedPaymentResponse(ret = recommendedPayment).toMono()
        ).thenReturn(
            GetRecommendedPaymentResponse(ret = recommendedPaymentAndBalanceDiff).toMono()
        )
        return captor
    }

    private fun mockGetAccount(promiseCredit: BigDecimal, email: String) {
        whenever(
            billingWebClient.getAccount(any())
        ).thenReturn(
            GetAccountResponse(
                ret = GetAccountRet(
                    account = Account(
                        email = email
                    ),
                    agreements = listOf(
                        AccountAgreement(
                            promiseCredit = promiseCredit
                        )
                    )
                )
            ).toMono()
        )
    }

    private fun mockGetPaymentsEmpty() {
        whenever(
            billingWebClient.getPayments(any())
        ).thenReturn(
            GetPaymentsResponse().toMono()
        )
    }

    private fun mockClientPromisePaymentSuccess() {
        whenever(
            billingWebClient.clientPromisePayment(any(), any())
        ).thenReturn(
            BillingResponseItem(data = ClientPromisePaymentResponse(ret = 1L)).toMono()
        )
    }
}