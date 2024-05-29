package su.vshk.billing.bot.scheduler

import com.nhaarman.mockitokotlin2.*
import org.assertj.core.api.Assertions.assertThat
import su.vshk.billing.bot.Bot
import su.vshk.billing.bot.dao.model.UserEntity
import su.vshk.billing.bot.dao.repository.UserRepository
import su.vshk.billing.bot.web.client.BillingWebClient
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.telegram.telegrambots.meta.api.objects.Message
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import su.vshk.billing.bot.config.BotProperties
import su.vshk.billing.bot.dao.model.PaymentNotificationEntity
import su.vshk.billing.bot.dao.model.PaymentNotificationMessageEntity
import su.vshk.billing.bot.dao.model.PaymentNotificationType
import su.vshk.billing.bot.dao.repository.PaymentNotificationMessageRepository
import su.vshk.billing.bot.dao.repository.PaymentNotificationRepository
import su.vshk.billing.bot.message.dto.ResponseMessageItem
import su.vshk.billing.bot.web.dto.manager.*
import java.math.BigDecimal

@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PaymentNotificationSchedulerTest {

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var paymentNotificationRepository: PaymentNotificationRepository

    @Autowired
    private lateinit var paymentNotificationMessageRepository: PaymentNotificationMessageRepository

    @Autowired
    private lateinit var paymentScheduler: PaymentScheduler

    @Autowired
    private lateinit var properties: BotProperties

    @MockBean
    private lateinit var billingWebClient: BillingWebClient

    @SpyBean
    private lateinit var bot: Bot

    @BeforeEach
    fun setUp() {
        userRepository.deleteAll()
        paymentNotificationRepository.deleteAll()
        paymentNotificationMessageRepository.deleteAll()
    }

    @Test
    fun testSendFirstToLastDaysPaymentNotification() {
        val sendMessageChatIdCaptor = argumentCaptor<Long>()
        val sendMessageResponseContentCaptor = argumentCaptor<ResponseMessageItem.Content>()

        doReturn(Message().also { it.messageId = 11 }.toMono())
            .doReturn(Message().also { it.messageId = 21 }.toMono())
            .whenever(bot).sendMessage(sendMessageChatIdCaptor.capture(), sendMessageResponseContentCaptor.capture())

        preSaveUsers()
        val (vgroupsCaptor, recommendedPaymentCaptor) = mockGetVgroupsAndRecommendedPayment()

        paymentScheduler.sendOneDayPaymentNotification()
        Thread.sleep(10_000L)

        vgroupsCaptor.allValues.let { args ->
            assertThat(args.size).isEqualTo(3)
            assertThat(args[0].filter?.userId).isEqualTo(10L)
            assertThat(args[1].filter?.userId).isEqualTo(20L)
            assertThat(args[2].filter?.userId).isEqualTo(30L)
        }

        recommendedPaymentCaptor.allValues.let { args ->
            assertThat(args.size).isEqualTo(4)
            assertThat(args[0].agreementId).isEqualTo(10L)
            assertThat(args[0].mode).isEqualTo(1L)
            assertThat(args[1].agreementId).isEqualTo(20L)
            assertThat(args[1].mode).isEqualTo(1L)
            assertThat(args[2].agreementId).isEqualTo(21L)
            assertThat(args[2].mode).isEqualTo(1L)
            assertThat(args[3].agreementId).isEqualTo(30L)
            assertThat(args[3].mode).isEqualTo(1L)
        }

        assertThat(sendMessageChatIdCaptor.allValues).containsExactly(properties.errorGroupNotification.chatId, 2)
        sendMessageResponseContentCaptor.allValues.map { it.text }.let { messages ->
            assertThat(messages.size).isEqualTo(2)
            assertThat(messages[0]).contains("Код ошибки:")
            assertThat(
                messages[1]
            ).contains(
                "Напоминаем Вам, что до окончания оплаченного периода",
                "остался 1 день",
                "По адресу:",
                "кв-л Лесной, дом 8",
                "200 ₽",
                "2000 ₽",
                "кв-л Жилой, дом 7",
                "300 ₽",
                "3000 ₽",
            )
        }

        paymentNotificationMessageRepository.findAll().let { entities ->
            assertThat(entities.size).isEqualTo(1)

            assertThat(
                entities.any { it.telegramId == 2L && it.messageId == 21 }
            ).isTrue
        }
    }

    @Test
    fun testDeleteFirstToLastDaysPaymentNotification() {
        val deleteMessageChatIdCaptor = argumentCaptor<Long>()
        val deleteMessageIdsCaptor = argumentCaptor<List<Int>>()

        doReturn(Mono.empty<Unit>())
            .whenever(bot).deleteMessages(deleteMessageChatIdCaptor.capture(), deleteMessageIdsCaptor.capture())

        preSavePaymentNotificationMessages()

        paymentScheduler.deleteOneDayPaymentNotification()
        Thread.sleep(2000L)

        assertThat(deleteMessageChatIdCaptor.allValues).containsExactly(1, 2)
        deleteMessageIdsCaptor.allValues.let { messages ->
            assertThat(messages.size).isEqualTo(2)
            assertThat(messages[0]).containsExactly(1)
            assertThat(messages[1]).containsExactly(2)
        }

        assertThat(
            paymentNotificationMessageRepository.findAll()
        ).isEmpty()
    }

    private fun preSaveUsers() {
        listOf(
            // будет выкинута ошибка
            UserEntity(
                telegramId = 1L,
                userId = 10L,
                login = "jack",
                agreementId = 10L
            ),
            // отправим напоминание
            UserEntity(
                telegramId = 2L,
                userId = 20L,
                login = "john",
                agreementId = 20L
            ),
            // actualRecommendPayment == 0
            UserEntity(
                telegramId = 3L,
                userId = 30L,
                login = "jason",
                agreementId = 30L
            ),
            // notification disabled
            UserEntity(
                telegramId = 4L,
                userId = 40L,
                login = "jimbo",
                agreementId = 40L
            )
        ).let { userRepository.saveAll(it) }

        listOf(
            PaymentNotificationEntity(
                telegramId = 1L,
                notificationType = PaymentNotificationType.SINGLE
            ),
            PaymentNotificationEntity(
                telegramId = 2L,
                notificationType = PaymentNotificationType.ALL
            ),
            PaymentNotificationEntity(
                telegramId = 3L,
                notificationType = PaymentNotificationType.SINGLE
            )
        ).let { paymentNotificationRepository.saveAll(it) }
    }

    private fun mockGetVgroupsAndRecommendedPayment(): Pair<KArgumentCaptor<GetVgroupsRequest>, KArgumentCaptor<GetRecommendedPaymentRequest>> {
        val vgroupsCaptor = argumentCaptor<GetVgroupsRequest>()
        val recommendedPaymentCaptor = argumentCaptor<GetRecommendedPaymentRequest>()

        whenever(
            billingWebClient.getVgroups(vgroupsCaptor.capture())
        ).thenReturn(
            GetVgroupsResponse(
                listOf(
                    GetVgroupsRet(
                        balance = BigDecimal("100"),
                        agentDescription = "Netflow",
                        username = "",
                        agreementId = 10L,
                        agreementNumber = "Int-1111/11",
                        addresses = listOf(
                            GetVgroupsAddress(
                                address = ""
                            )
                        )
                    )
                )
            ).toMono()
        ).thenReturn(
            GetVgroupsResponse(
                listOf(
                    GetVgroupsRet(
                        balance = BigDecimal("200"),
                        agentDescription = "Netflow",
                        username = "",
                        agreementId = 20L,
                        agreementNumber = "Int-2222/22",
                        addresses = listOf(
                            GetVgroupsAddress(
                                address = "Россия,обл Московская,р-н Щелковский,,,кв-л Лесной,дом 8,,,,141181"
                            )
                        )
                    ),
                    GetVgroupsRet(
                        balance = BigDecimal("300"),
                        agentDescription = "Netflow",
                        username = "",
                        agreementId = 21L,
                        agreementNumber = "Int-3333/33",
                        addresses = listOf(
                            GetVgroupsAddress(
                                address = "Россия,обл Московская,р-н Щелковский,,,кв-л Жилой,дом 7,,,,141181"
                            )
                        )
                    )
                )
            ).toMono()
        ).thenReturn(
            GetVgroupsResponse(
                listOf(
                    GetVgroupsRet(
                        balance = BigDecimal("400"),
                        agentDescription = "Netflow",
                        username = "",
                        agreementId = 30L,
                        agreementNumber = "Int-4444/44",
                        addresses = listOf(
                            GetVgroupsAddress(
                                address = ""
                            )
                        )
                    )
                )
            ).toMono()
        )

        whenever(
            billingWebClient.getRecommendedPayment(recommendedPaymentCaptor.capture())
        ).thenThrow(
            RuntimeException("error occurs")
        ).thenReturn(
            GetRecommendedPaymentResponse(BigDecimal("2000")).toMono()
        ).thenReturn(
            GetRecommendedPaymentResponse(BigDecimal("3000")).toMono()
        ).thenReturn(
            GetRecommendedPaymentResponse(BigDecimal("0")).toMono()
        )

        return Pair(vgroupsCaptor, recommendedPaymentCaptor)
    }

    private fun preSavePaymentNotificationMessages() {
        listOf(
            PaymentNotificationMessageEntity(
                telegramId = 1L,
                messageId = 1
            ),
            PaymentNotificationMessageEntity(
                telegramId = 2L,
                messageId = 2
            )
        ).let { paymentNotificationMessageRepository.saveAll(it) }
    }
}