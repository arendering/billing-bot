package su.vshk.billing.bot

import com.nhaarman.mockitokotlin2.*
import org.assertj.core.api.Assertions.assertThat
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
import su.vshk.billing.bot.dao.model.*
import su.vshk.billing.bot.dao.repository.PaymentNotificationMessageRepository
import su.vshk.billing.bot.dao.repository.PaymentNotificationRepository
import su.vshk.billing.bot.dialog.option.*
import su.vshk.billing.bot.message.dto.ResponseMessageItem
import su.vshk.billing.bot.service.LoginMessageIdService
import su.vshk.billing.bot.util.getLogger
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
    private lateinit var paymentNotificationRepository: PaymentNotificationRepository

    @Autowired
    private lateinit var loginMessageIdService: LoginMessageIdService

    @Autowired
    private lateinit var properties: BotProperties

    @SpyBean
    private lateinit var bot: Bot

    @MockBean
    private lateinit var billingWebClient: BillingWebClient

    private val logger = getLogger()
    private val telegramId = 42L
    private val buttonRequestMessageId = 1

    @BeforeEach
    fun setUp() {
        userRepository.deleteAll()
        paymentNotificationMessageRepository.deleteAll()
        paymentNotificationRepository.deleteAll()
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

        val userId = 13L
        mockGetClientId(userId)

        val agreementId = 999L
        mockGetVgroups(agreementId = agreementId)

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
                assertThat(this.filter?.userId).isEqualTo(userId)
                true
            }
        )

        val user = userRepository.findAll().let {
            assertThat(it.size).isEqualTo(1)
            it[0]
        }
        assertThat(user.telegramId).isEqualTo(telegramId)
        assertThat(user.userId).isEqualTo(userId)
        assertThat(user.login).isEqualTo("user")
        assertThat(user.agreementId).isEqualTo(agreementId)

        assertThat(paymentNotificationMessageRepository.findAll()).isEmpty()

        assertThat(loginMessageIdService.isEmpty(user.telegramId)).isTrue
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

        sendText(data = Command.LOGIN.value, requestMessageId = 10)
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
        assertThat(loginMessageIdService.isEmpty(telegramId)).isTrue
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

    /*
    Зарегистрированный пользователь ввел текст "/start".
    Нужно отправить ему еще раз сообщение с меню, т.к он мог удалить предыдущее сообщение с меню.
     */
    @Test
    fun testRepeatMenuIfRegisteredUserSendStartAsText() {
        preSaveUser()

        val sendMessageChatIdCaptor = argumentCaptor<Long>()
        val sendMessageResponseContentCaptor = argumentCaptor<ResponseMessageItem.Content>()

        doReturn(Message().also { it.messageId = 11 }.toMono())
            .whenever(bot).sendMessage(sendMessageChatIdCaptor.capture(), sendMessageResponseContentCaptor.capture())

        val deleteMessageChatIdCaptor = argumentCaptor<Long>()
        val deleteMessageIdsCaptor = argumentCaptor<List<Int>>()

        doReturn(Mono.empty<Unit>())
            .whenever(bot).deleteMessages(deleteMessageChatIdCaptor.capture(), deleteMessageIdsCaptor.capture())

        sendText(data = Command.LOGIN.value, requestMessageId = 10)

        assertThat(sendMessageChatIdCaptor.allValues).containsExactly(telegramId)
        sendMessageResponseContentCaptor.allValues.map { it.text }.let { messages ->
            assertThat(messages.size).isEqualTo(1)
            assertThat(messages[0]).contains("Главное меню")
        }

        assertThat(deleteMessageChatIdCaptor.firstValue).isEqualTo(telegramId)
        assertThat(deleteMessageIdsCaptor.firstValue).containsExactly(10)
    }

    /*
    Пользователь, находясь в диалоге, ввел текст "/start".
    Нужно отправить ему еще раз предыдущее сообщение, которое было в диалоге, т.к он мог его удалить.
     */
    @Test
    fun testRepeatMenuIfDialogUserSendStartAsText() {
        val editMessageChatIdCaptor = argumentCaptor<Long>()
        val editMessageMessageIdCaptor = argumentCaptor<Int>()
        val editMessageResponseContentCaptor = argumentCaptor<ResponseMessageItem.Content>()

        doReturn(Mono.empty<Unit>())
            .doReturn(Mono.empty<Unit>())
            .whenever(bot).editMessage(editMessageChatIdCaptor.capture(), editMessageMessageIdCaptor.capture(), editMessageResponseContentCaptor.capture())

        val sendMessageChatIdCaptor = argumentCaptor<Long>()
        val sendMessageResponseContentCaptor = argumentCaptor<ResponseMessageItem.Content>()

        doReturn(Message().also { it.messageId = 11 }.toMono())
            .whenever(bot).sendMessage(sendMessageChatIdCaptor.capture(), sendMessageResponseContentCaptor.capture())

        val deleteMessageChatIdCaptor = argumentCaptor<Long>()
        val deleteMessageIdsCaptor = argumentCaptor<List<Int>>()

        doReturn(Mono.empty<Unit>())
            .whenever(bot).deleteMessages(deleteMessageChatIdCaptor.capture(), deleteMessageIdsCaptor.capture())

        preSaveUser()
        mockGetPaymentsEmpty()

        pushButton(Command.PAYMENT_HISTORY.value)
        sendText(data = Command.LOGIN.value, requestMessageId = 10)
        pushButton(PaymentHistoryAvailableOptions.PERIOD_ONE_MONTH)

        assertThat(editMessageChatIdCaptor.allValues).containsExactly(telegramId, telegramId)
        assertThat(editMessageMessageIdCaptor.allValues).containsExactly(buttonRequestMessageId, buttonRequestMessageId)
        editMessageResponseContentCaptor.allValues.map { it.text }.let { messages ->
            assertThat(messages.size).isEqualTo(2)
            assertThat(messages[0]).contains("За какой период времени Вы хотите увидеть список платежей ?")
            assertThat(messages[1]).contains("Платежи с", "Нет данных.")
        }

        assertThat(sendMessageChatIdCaptor.firstValue).isEqualTo(telegramId)
        assertThat(sendMessageResponseContentCaptor.firstValue.text).contains("За какой период времени Вы хотите увидеть список платежей ?")

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

        sendText(data = Command.LOGIN.value, requestMessageId = 10)
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
        assertThat(loginMessageIdService.isEmpty(telegramId)).isTrue
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

        pushButton(Command.AGREEMENTS.value)

        assertThat(sendMessageChatIdCaptor.firstValue).isEqualTo(properties.errorGroupNotification.chatId)
        assertThat(sendMessageResponseContentCaptor.firstValue.text).contains("Код ошибки:")

        assertThat(editMessageChatIdCaptor.firstValue).isEqualTo(telegramId)
        assertThat(editMessageMessageIdCaptor.firstValue).isEqualTo(buttonRequestMessageId)
        assertThat(editMessageResponseContentCaptor.firstValue.text).contains("К сожалению, на данный момент функция недоступна")
    }

    /*
    Может произойти, если бот перезагрузился, а пользователь был в диалоге.
    Тогда пользователь введет опцию с кнопки, а бот ожидает ввода команды.
     */
    @Test
    fun testUnexpectedCommandInput() {
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

    /*
    Пользователь успешно выходит из бота.
    При этом у него в чате есть напоминание об оплате, которое тоже будет удалено.
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
    fun testAgreements() {
        val editMessageChatIdCaptor = argumentCaptor<Long>()
        val editMessageMessageIdCaptor = argumentCaptor<Int>()
        val editMessageResponseContentCaptor = argumentCaptor<ResponseMessageItem.Content>()

        doReturn(Mono.empty<Unit>())
            .doReturn(Mono.empty<Unit>())
            .doReturn(Mono.empty<Unit>())
            .whenever(bot).editMessage(editMessageChatIdCaptor.capture(), editMessageMessageIdCaptor.capture(), editMessageResponseContentCaptor.capture())

        preSaveUser()
        mockGetVgroups(
            GetVgroupsResponse(
                ret = listOf(
                    GetVgroupsRet(
                        username = "Иван Иванов",
                        agreementId = 999,
                        agreementNumber = "Int-1111/11",
                        balance = BigDecimal("100"),
                        agentDescription = "Netflow",
                        addresses = listOf(
                            GetVgroupsAddress(
                                address = "Россия,обл Московская,р-н Щелковский,,,кв-л Лесной,дом 8,,,,141181"
                            )
                        ),
                        blocked = 1
                    ),
                    GetVgroupsRet(
                        username = "Иван Иванов",
                        agreementId = 998,
                        agreementNumber = "Int-2222/22",
                        balance = BigDecimal("100"),
                        agentDescription = "Netflow",
                        addresses = listOf(
                            GetVgroupsAddress(
                                address = "Россия,обл Московская,р-н Щелковский,,,кв-л Лесной,дом 9,,,,141181"
                            )
                        )
                    )
                )
            )
        )

        val recommendedPaymentCaptor = mockGetRecommendedPayment(
            defaultRecommendedPayment = BigDecimal("150"),
            actualRecommendedPayment = BigDecimal("50")
        )

        mockGetAccount()

        pushButton(Command.AGREEMENTS.value)
        pushButton(AgreementAvailableOptions.SWITCH_AGREEMENT)
        pushButton("998")


        assertThat(editMessageChatIdCaptor.allValues).containsExactly(telegramId, telegramId, telegramId)
        assertThat(editMessageMessageIdCaptor.allValues).containsExactly(buttonRequestMessageId, buttonRequestMessageId, buttonRequestMessageId)
        editMessageResponseContentCaptor.allValues.map { it.text }.let { messages ->
            assertThat(messages.size).isEqualTo(3)
            assertThat(messages[0]).contains(
                "ФИО",
                "Иван Иванов",
                "Номер договора",
                "Int-1111/11",
                "Статус интернета",
                "УЗ заблокирована по балансу",
                "Баланс",
                "100 ₽",
                "Ежемесячный платеж",
                "150 ₽",
                "Рекомендуется внести",
                "50 ₽",
                "E-mail",
                "Не указан"
            )

            assertThat(messages[1]).contains(
                "Выберите договор, с которым будет производиться дальнейшее взаимодействие",
                "Ваши договоры:",
                "Int-1111/11",
                "р-н Щелковский, кв-л Лесной, дом 8",
                "Int-2222/22",
                "р-н Щелковский, кв-л Лесной, дом 9"
            )

            assertThat(messages[2]).contains(
                "Вами был выбран договор",
                "Int-2222/22",
                "по адресу",
                "р-н Щелковский, кв-л Лесной, дом 9"
            )
        }

        verify(billingWebClient).getVgroups(
            argThat {
                assertThat(this.filter?.userId).isEqualTo(13L)
                true
            }
        )

        recommendedPaymentCaptor.allValues.let { requests ->
            assertThat(requests.size).isEqualTo(2)
            assertThat(requests[0].agreementId).isEqualTo(999L)
            assertThat(requests[0].mode).isEqualTo(0L)

            assertThat(requests[1].agreementId).isEqualTo(999L)
            assertThat(requests[1].mode).isEqualTo(1L)
        }

        verify(billingWebClient).getAccount(
            argThat {
                assertThat(this.userId).isEqualTo(13L)
                true
            }
        )

        userRepository.findById(telegramId).orElse(null).let { u ->
            assertThat(u).isNotNull
            assertThat(u.agreementId).isEqualTo(998)
        }
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
    fun testPaymentHistory() {
        val editMessageChatIdCaptor = argumentCaptor<Long>()
        val editMessageMessageIdCaptor = argumentCaptor<Int>()
        val editMessageResponseContentCaptor = argumentCaptor<ResponseMessageItem.Content>()

        doReturn(Mono.empty<Unit>())
            .doReturn(Mono.empty<Unit>())
            .whenever(bot).editMessage(editMessageChatIdCaptor.capture(), editMessageMessageIdCaptor.capture(), editMessageResponseContentCaptor.capture())

        preSaveUser()
        mockGetPaymentsEmpty()

        pushButton(Command.PAYMENT_HISTORY.value)
        pushButton(PaymentHistoryAvailableOptions.PERIOD_ONE_MONTH)

        assertThat(editMessageChatIdCaptor.allValues).containsExactly(telegramId, telegramId)
        assertThat(editMessageMessageIdCaptor.allValues).containsExactly(buttonRequestMessageId, buttonRequestMessageId)
        editMessageResponseContentCaptor.allValues.map { it.text }.let { messages ->
            assertThat(messages.size).isEqualTo(2)
            assertThat(messages[0]).contains("За какой период времени Вы хотите увидеть список платежей ?")
            assertThat(messages[1]).contains("Платежи с", "Нет данных.")
        }

        verify(billingWebClient).getPayments(
            argThat {
                val flt = this.filter!!
                assertThat(flt.agreementId).isEqualTo(999L)
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
                assertThat(agreementId).isEqualTo(999L)
                true
            }
        )

        verify(billingWebClient).clientPromisePayment(
            argThat {
                assertThat(this.telegramId).isEqualTo(user.telegramId)
                assertThat(this.userId).isEqualTo(user.userId)
                assertThat(this.login).isEqualTo(user.login)
                assertThat(this.agreementId).isEqualTo(user.agreementId)
                true
            },
            argThat {
                assertThat(this.agreementId).isEqualTo(999L)
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
            GetVgroupsResponse(
                ret = listOf(
                    GetVgroupsRet(
                        agreementId = 999,
                        tariffId = 10
                    ),
                    GetVgroupsRet(
                        agreementId = 999,
                        tariffId = 11
                    ),
                    GetVgroupsRet(
                        agreementId = 999,
                        tariffId = 12,
                        tariffDescription = "ТВ - (Архив) Смотрешка 25 за 25"
                    ),
                    GetVgroupsRet(
                        agreementId = 999,
                        tariffId = 13
                    )
                )
            )
        )

        mockGetSbssKnowledge(
            GetSbssKnowledgeResponse(
                ret = GetSbssKnowledgeRet(
                    posts = listOf(
                        GetSbssKnowledgePostFull(
                            post = GetSbssKnowledgePost(
                                text = "tarid: 10, type: internet, name: Свои, speed: 1Gbit/s, rent: 100р/мес, client: физическое лицо."
                            )
                        ),
                        GetSbssKnowledgePostFull(
                            post = GetSbssKnowledgePost(
                                text = "tarid: 11, type: tv, name: 24ТВ \"Лайт +\", channels: 241 канал, rent: 199р/мес."
                            )
                        ),
                        GetSbssKnowledgePostFull(
                            post = GetSbssKnowledgePost(
                                text = "tarid: 13, type: internet-tv, name: Мега+ТВ, speed: 500Mbit/s, channels: 144 канала, rent: 1200р/мес."
                            )
                        )
                    )
                )
            )
        )

        pushButton(Command.TARIFFS.value)

        assertThat(editMessageChatIdCaptor.firstValue).isEqualTo(telegramId)
        assertThat(editMessageMessageIdCaptor.firstValue).isEqualTo(buttonRequestMessageId)
        editMessageResponseContentCaptor.firstValue.text.let { text ->
            logger.info("Response message: $text")
            assertThat(text).contains(
                "Интернет:", "Тариф", "Свои", "Скорость", "1Gbit/s", "Стоимость", "100р/мес",
                "Онлайн ТВ:", "Тариф", "24ТВ \"Лайт +\"", "Каналов", "241", "Стоимость", "199р/мес.", "(Архив) Смотрешка 25 за 25",
                "Комбо (Интернет + ТВ)", "Тариф", "Мега+ТВ", "Скорость", "500Mbit/s", "Каналов", "144", "Стоимость", "1200р/мес.",
                "Вы используете архивные тарифные опции! Рекомендуем перейти на более выгодные и актуальные тарифы"
            )
        }
    }

    @Test
    fun testEnableNotificationForAllAgreements() {
        val editMessageChatIdCaptor = argumentCaptor<Long>()
        val editMessageMessageIdCaptor = argumentCaptor<Int>()
        val editMessageResponseContentCaptor = argumentCaptor<ResponseMessageItem.Content>()

        doReturn(Mono.empty<Unit>())
            .doReturn(Mono.empty<Unit>())
            .whenever(bot).editMessage(editMessageChatIdCaptor.capture(), editMessageMessageIdCaptor.capture(), editMessageResponseContentCaptor.capture())

        preSaveUser()
        mockGetVgroups(
            GetVgroupsResponse(
                ret = listOf(
                    GetVgroupsRet(agreementId = 999, agentDescription = "Netflow"),
                    GetVgroupsRet(agreementId = 888, agentDescription = "Netflow")
                )
            )
        )

        pushButton(Command.NOTIFICATION.value)
        pushButton(NotificationAvailableOptions.ENABLE_FOR_ALL_AGREEMENTS)

        assertThat(editMessageChatIdCaptor.allValues).containsExactly(telegramId, telegramId)
        assertThat(editMessageMessageIdCaptor.allValues).containsExactly(buttonRequestMessageId, buttonRequestMessageId)
        editMessageResponseContentCaptor.allValues.map { it.text }.let { messages ->
            assertThat(messages.size).isEqualTo(2)
            assertThat(
                messages[0]
            ).contains(
                "На данный момент у Вас отключены оповещения об окончании ежемесячного расчетного периода.",
                "Выберите подходящий вариант:",
                "Подключить оповещения для текущего договора",
                "Подключить оповещения для всех Ваших договоров"
            )
            assertThat(
                messages[1]
            ).contains(
                "Оповещения по всем договорам в нашей сети успешно подключены.",
                "Оповещения, касаемо каждого договора отдельно, не поступят в случае достаточной суммы средств на Вашем балансе"
            )
        }

        paymentNotificationRepository.findById(telegramId).let { entityOpt ->
            assertThat(entityOpt.isPresent).isTrue()
            assertThat(entityOpt.get().notificationType).isEqualTo(PaymentNotificationType.ALL)
        }
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

        pushButton(Command.PAYMENT_HISTORY.value)
        pushButton(GenericCommand.DELETE_PAYMENT_NOTIFICATION)
        pushButton(PaymentHistoryAvailableOptions.PERIOD_ONE_MONTH)

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
                val flt = this.filter!!
                assertThat(flt.agreementId).isEqualTo(999L)
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
            userId = 13L,
            login = "user",
            agreementId = 999L
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

    private fun mockGetClientId(userId: Long?) {
        whenever(
            billingWebClient.getClientId(any())
        ).thenReturn(
            Optional.ofNullable(userId).toMono()
        )
    }

    private fun mockGetClientIdError() {
        whenever(
            billingWebClient.getClientId(any())
        ).thenThrow(
            RuntimeException("some error occurs")
        )
    }

    private fun mockGetVgroups(response: GetVgroupsResponse) {
        whenever(
            billingWebClient.getVgroups(any())
        ).thenReturn(
            response.toMono()
        )
    }

    private fun mockGetVgroups(
        username: String? = null,
        agreementNumber: String? = null,
        agreementId: Long? = null,
        balance: BigDecimal? = null
    ) {
        whenever(
            billingWebClient.getVgroups(any())
        ).thenReturn(
            GetVgroupsResponse(
                ret = listOf(
                    GetVgroupsRet(
                        username = username,
                        agreementNumber = agreementNumber,
                        agreementId = agreementId,
                        balance = balance,
                        agentDescription = "Netflow",
                    )
                )
            ).toMono()
        )
    }

    private fun mockGetSbssKnowledge(response: GetSbssKnowledgeResponse) {
        whenever(
            billingWebClient.getSbssKnowledge(any())
        ).thenReturn(
            response.toMono()
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
            GetRecommendedPaymentResponse(amount = recommendedPayment).toMono()
        )
    }

    private fun mockGetRecommendedPayment(
        defaultRecommendedPayment: BigDecimal,
        actualRecommendedPayment: BigDecimal
    ): KArgumentCaptor<GetRecommendedPaymentRequest> {
        val captor = argumentCaptor<GetRecommendedPaymentRequest>()
        whenever(
            billingWebClient.getRecommendedPayment(captor.capture())
        ).thenReturn(
            GetRecommendedPaymentResponse(amount = defaultRecommendedPayment).toMono()
        ).thenReturn(
            GetRecommendedPaymentResponse(amount = actualRecommendedPayment).toMono()
        )
        return captor
    }

    private fun mockGetAccount(promiseCredit: BigDecimal? = null, email: String? = null) {
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