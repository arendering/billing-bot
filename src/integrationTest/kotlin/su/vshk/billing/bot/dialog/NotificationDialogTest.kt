package su.vshk.billing.bot.dialog

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import reactor.kotlin.core.publisher.toMono
import reactor.kotlin.test.test
import su.vshk.billing.bot.dao.model.Command
import su.vshk.billing.bot.dao.model.PaymentNotificationEntity
import su.vshk.billing.bot.dao.model.PaymentNotificationType
import su.vshk.billing.bot.dao.repository.PaymentNotificationRepository
import su.vshk.billing.bot.dialog.dto.StateDto
import su.vshk.billing.bot.dialog.option.NotificationAvailableOptions
import su.vshk.billing.bot.dialog.option.NotificationOptions
import su.vshk.billing.bot.web.dto.manager.GetVgroupsResponse
import su.vshk.billing.bot.web.dto.manager.GetVgroupsRet

class NotificationDialogTest: BaseDialogTest() {

    @Autowired
    private lateinit var paymentNotificationRepository: PaymentNotificationRepository

    private val command = Command.NOTIFICATION

    @BeforeEach
    fun setUp() {
        paymentNotificationRepository.deleteAll()
    }

    @Test
    fun testDisableNotificationForSingleAgreement() {
        preSaveNotification(type = PaymentNotificationType.SINGLE)
        mockGetVgroupsWithSingleAgreement()

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
                    .assertResponseTextContains("На данный момент у Вас включены оповещения об оплате. Хотите отключить ?")
                    .assertButtonLabelsContains("Отключить", "⬅ Вернуться в главное меню")
                    .assertButtonCallbackDataContains("disable", "/cancel")
                true
            }
            .verifyComplete()

        assertDialogContainsUser()

        dialogProcessor
            .processOption(
                createRequest(input = NotificationAvailableOptions.DISABLE)
            )
            .test()
            .expectNextMatches {
                it.state
                    .assertFlags(isFinishedExpected = true, isCanceledExpected = false)
                    .assertSwitchOption(NotificationAvailableOptions.DISABLE)
                    .assertResponseMessageIsNull()
                true
            }
            .verifyComplete()

        assertDialogDoesNotContainUser()
    }

    @Test
    fun testEnableForAllAgreementsOrDisableNotification() {
        preSaveNotification(type = PaymentNotificationType.SINGLE)
        mockGetVgroupsWithMultipleAgreements()

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
                    .assertResponseTextContains("На данный момент у Вас подключены оповещения по текущему договору. Хотите подключить оповещения по всем Вашим договорам или отключить рассылку ?")
                    .assertButtonLabelsContains("Включить для всех договоров", "Отключить", "⬅ Вернуться в главное меню")
                    .assertButtonCallbackDataContains("enable_for_all_agreements", "disable", "/cancel")
                true
            }
            .verifyComplete()

        assertDialogContainsUser()

        dialogProcessor
            .processOption(
                createRequest(input = NotificationAvailableOptions.ENABLE_FOR_ALL_AGREEMENTS)
            )
            .test()
            .expectNextMatches {
                it.state
                    .assertFlags(isFinishedExpected = true, isCanceledExpected = false)
                    .assertSwitchOption(NotificationAvailableOptions.ENABLE_FOR_ALL_AGREEMENTS)
                    .assertResponseMessageIsNull()
                true
            }
            .verifyComplete()

        assertDialogDoesNotContainUser()
    }

    @Test
    fun testEnableForSingleAgreementOrDisableNotification() {
        preSaveNotification(type = PaymentNotificationType.ALL)
        mockGetVgroupsWithMultipleAgreements()

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
                    .assertResponseTextContains("На данный момент у Вас подключены оповещения по всем договорам в нашей сети. Хотите подключить оповещения только по текущему договору или отключить рассылку ?")
                    .assertButtonLabelsContains("Включить для текущего договора", "Отключить", "⬅ Вернуться в главное меню")
                    .assertButtonCallbackDataContains("enable_for_single_agreement", "disable", "/cancel")
                true
            }
            .verifyComplete()

        assertDialogContainsUser()

        dialogProcessor
            .processOption(
                createRequest(input = NotificationAvailableOptions.ENABLE_FOR_SINGLE_AGREEMENT)
            )
            .test()
            .expectNextMatches {
                it.state
                    .assertFlags(isFinishedExpected = true, isCanceledExpected = false)
                    .assertSwitchOption(NotificationAvailableOptions.ENABLE_FOR_SINGLE_AGREEMENT)
                    .assertResponseMessageIsNull()
                true
            }
            .verifyComplete()

        assertDialogDoesNotContainUser()
    }

    @Test
    fun testEnableNotification() {
        mockGetVgroupsWithSingleAgreement()

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
                    .assertResponseTextContains("На данный момент у Вас отключены оповещения об окончании ежемесячного расчетного периода. Хотите включить ")
                    .assertButtonLabelsContains("Включить", "⬅ Вернуться в главное меню")
                    .assertButtonCallbackDataContains("enable", "/cancel")
                true
            }
            .verifyComplete()

        assertDialogContainsUser()

        dialogProcessor
            .processOption(
                createRequest(input = NotificationAvailableOptions.ENABLE)
            )
            .test()
            .expectNextMatches {
                it.state
                    .assertFlags(isFinishedExpected = true, isCanceledExpected = false)
                    .assertSwitchOption(NotificationAvailableOptions.ENABLE)
                    .assertResponseMessageIsNull()
                true
            }
            .verifyComplete()

        assertDialogDoesNotContainUser()
    }

    @Test
    fun testEnableForSingleAgreementOrEnableForAllAgreements() {
        mockGetVgroupsWithMultipleAgreements()

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
                        "На данный момент у Вас отключены оповещения об окончании ежемесячного расчетного периода.",
                        "Выберите подходящий вариант:",
                        "Подключить оповещения для текущего договора",
                        "Подключить оповещения для всех Ваших договоров"
                    )
                    .assertButtonLabelsContains("Включить для текущего договора", "Включить для всех договоров", "⬅ Вернуться в главное меню")
                    .assertButtonCallbackDataContains("enable_for_single_agreement", "enable_for_all_agreements", "/cancel")
                true
            }
            .verifyComplete()

        assertDialogContainsUser()

        dialogProcessor
            .processOption(
                createRequest(input = NotificationAvailableOptions.ENABLE_FOR_SINGLE_AGREEMENT)
            )
            .test()
            .expectNextMatches {
                it.state
                    .assertFlags(isFinishedExpected = true, isCanceledExpected = false)
                    .assertSwitchOption(NotificationAvailableOptions.ENABLE_FOR_SINGLE_AGREEMENT)
                    .assertResponseMessageIsNull()
                true
            }
            .verifyComplete()

        assertDialogDoesNotContainUser()
    }

    private fun preSaveNotification(type: String) {
        paymentNotificationRepository.save(
            PaymentNotificationEntity(
                telegramId = telegramId,
                notificationType = type
            )
        )
    }

    private fun mockGetVgroupsWithSingleAgreement() {
        whenever(
            billingWebClient.getVgroups(any())
        ).thenReturn(
            GetVgroupsResponse(
                ret = listOf(
                    GetVgroupsRet(
                        agreementId = 80,
                        agentDescription = "Netflow"
                    )
                )
            ).toMono()
        )
    }

    private fun mockGetVgroupsWithMultipleAgreements() {
        whenever(
            billingWebClient.getVgroups(any())
        ).thenReturn(
            GetVgroupsResponse(
                ret = listOf(
                    GetVgroupsRet(
                        agreementId = 80,
                        agentDescription = "Netflow"
                    ),
                    GetVgroupsRet(
                        agreementId = 81,
                        agentDescription = "Netflow"
                    )
                )
            ).toMono()
        )
    }

    private fun StateDto.assertSwitchOption(expected: String): StateDto {
        val options = this.options as NotificationOptions
        assertThat(options.switch).isEqualTo(expected)
        return this
    }
}