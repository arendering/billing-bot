package su.vshk.billing.bot.dialog

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import reactor.kotlin.core.publisher.toMono
import reactor.kotlin.test.test
import su.vshk.billing.bot.dao.model.Command
import su.vshk.billing.bot.dialog.dto.StateDto
import su.vshk.billing.bot.dialog.option.AgreementAvailableOptions
import su.vshk.billing.bot.dialog.option.AgreementOptions
import su.vshk.billing.bot.service.dto.AgreementDto
import su.vshk.billing.bot.web.dto.manager.*
import java.math.BigDecimal

class AgreementDialogTest: BaseDialogTest() {

    private val command = Command.AGREEMENTS

    @Test
    fun testSingleAgreement() {
        mockGetVgroups(
            GetVgroupsResponse(
                ret = listOf(
                    GetVgroupsRet(
                        username = "Иван Иванов",
                        agreementId = 80,
                        agreementNumber = "Int-1111/11",
                        balance = BigDecimal("100"),
                        agentDescription = "Netflow",
                        blocked = 0
                    )
                )
            )
        )

        mockGetRecommendedPayment(
            defaultPayment = BigDecimal("150"),
            actualPayment = BigDecimal("50")
        )

        mockGetAccount()

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
                        "ФИО",
                        "Иван Иванов",
                        "Номер договора",
                        "Int-1111/11",
                        "Статус интернета",
                        "Баланс",
                        "100 ₽",
                        "Ежемесячный платеж",
                        "150 ₽",
                        "Рекомендуется внести",
                        "50 ₽",
                        "E-mail",
                        "Не указан"
                    )
                    .assertButtonLabelsContains("⬅ Вернуться в главное меню")
                    .assertButtonCallbackDataContains("/menu")

                true
            }
            .verifyComplete()

        assertDialogDoesNotContainUser()
    }

    @Test
    fun testCancelSwitchAgreement() {
        mockGetVgroups(
            GetVgroupsResponse(
                ret = listOf(
                    GetVgroupsRet(
                        username = "Иван Иванов",
                        agreementId = 80,
                        agreementNumber = "Int-1111/11",
                        balance = BigDecimal("100"),
                        agentDescription = "Netflow",
                        addresses = listOf(
                            GetVgroupsAddress(
                                address = "Россия,обл Московская,р-н Щелковский,,,кв-л Лесной,дом 8,,,,141181"
                            )
                        ),
                        blocked = 0
                    ),
                    GetVgroupsRet(
                        username = "Иван Иванов",
                        agreementId = 81,
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

        mockGetRecommendedPayment(
            defaultPayment = BigDecimal("150"),
            actualPayment = BigDecimal("50")
        )

        mockGetAccount()

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
                        "ФИО",
                        "Иван Иванов",
                        "Номер договора",
                        "Int-1111/11",
                        "Статус интернета",
                        "Баланс",
                        "100 ₽",
                        "Ежемесячный платеж",
                        "150 ₽",
                        "Рекомендуется внести",
                        "50 ₽",
                        "E-mail",
                        "Не указан"
                    )
                    .assertButtonLabelsContains("Выбрать договор", "⬅ Вернуться в главное меню")
                    .assertButtonCallbackDataContains("/switch_agreement", "/cancel_info")

                true
            }
            .verifyComplete()

        assertDialogContainsUser()

        dialogProcessor
            .processOption(
                request = createRequest(input = AgreementAvailableOptions.SWITCH_AGREEMENT)
            )
            .test()
            .expectNextMatches {
                it.state
                    .assertFlags(isFinishedExpected = false, isCanceledExpected = false)
                    .assertResponseTextContains(
                        "Выберите договор, с которым будет производиться дальнейшее взаимодействие",
                        "Ваши договоры:",
                        "Int-1111/11",
                        "р-н Щелковский, кв-л Лесной, дом 8",
                        "Int-2222/22",
                        "р-н Щелковский, кв-л Лесной, дом 9"
                    )
                    .assertButtonLabelsContains("Выбрать договор № Int-2222/22", "⬅ Вернуться в главное меню")
                    .assertButtonCallbackDataContains("81", "/cancel_switch_agreement")
                true
            }
            .verifyComplete()

        assertDialogContainsUser()

        dialogProcessor
            .processOption(
                request = createRequest(input = AgreementAvailableOptions.CANCEL_SWITCH_AGREEMENT)
            )
            .test()
            .expectNextMatches {
                it.state
                    .assertFlags(isFinishedExpected = false, isCanceledExpected = true)
                    .assertResponseTextContains("Главное меню")
                true
            }
            .verifyComplete()

        assertDialogDoesNotContainUser()
    }

    @Test
    fun testSwitchAgreement() {
        mockGetVgroups(
            GetVgroupsResponse(
                ret = listOf(
                    GetVgroupsRet(
                        username = "Иван Иванов",
                        agreementId = 80,
                        agreementNumber = "Int-1111/11",
                        balance = BigDecimal("100"),
                        agentDescription = "Netflow",
                        addresses = listOf(
                            GetVgroupsAddress(
                                address = "Россия,обл Московская,р-н Щелковский,,,кв-л Лесной,дом 8,,,,141181"
                            )
                        ),
                        blocked = 0
                    ),
                    GetVgroupsRet(
                        username = "Иван Иванов",
                        agreementId = 81,
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

        mockGetRecommendedPayment(
            defaultPayment = BigDecimal("150"),
            actualPayment = BigDecimal("50")
        )

        mockGetAccount()

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
                        "ФИО",
                        "Иван Иванов",
                        "Номер договора",
                        "Int-1111/11",
                        "Статус интернета",
                        "Баланс",
                        "100 ₽",
                        "Ежемесячный платеж",
                        "150 ₽",
                        "Рекомендуется внести",
                        "50 ₽",
                        "E-mail",
                        "Не указан"
                    )
                    .assertButtonLabelsContains( "Выбрать договор", "⬅ Вернуться в главное меню")
                    .assertButtonCallbackDataContains("/switch_agreement", "/cancel_info")

                true
            }
            .verifyComplete()

        assertDialogContainsUser()

        dialogProcessor
            .processOption(
                request = createRequest(input = AgreementAvailableOptions.SWITCH_AGREEMENT)
            )
            .test()
            .expectNextMatches {
                it.state
                    .assertFlags(isFinishedExpected = false, isCanceledExpected = false)
                    .assertResponseTextContains(
                        "Выберите договор, с которым будет производиться дальнейшее взаимодействие",
                        "Ваши договоры:",
                        "Int-1111/11",
                        "р-н Щелковский, кв-л Лесной, дом 8",
                        "Int-2222/22",
                        "р-н Щелковский, кв-л Лесной, дом 9"
                    )
                    .assertButtonLabelsContains("⬅ Вернуться в главное меню", "Выбрать договор № Int-2222/22")
                    .assertButtonCallbackDataContains("/cancel_switch_agreement", "81")
                true
            }
            .verifyComplete()

        assertDialogContainsUser()

        dialogProcessor
            .processOption(
                request = createRequest(input = "81")
            )
            .test()
            .expectNextMatches {
                it.state
                    .assertFlags(isFinishedExpected = true, isCanceledExpected = false)
                    .assertResponseMessageIsNull()
                    .assertOptions(
                        expectedAgreement = AgreementDto(
                            agreementId = 81,
                            agreementNumber = "Int-2222/22",
                            address = "р-н Щелковский, кв-л Лесной, дом 9"
                        )
                    )
                true
            }
            .verifyComplete()

        assertDialogDoesNotContainUser()
    }

    private fun mockGetVgroups(response: GetVgroupsResponse) {
        whenever(
            billingWebClient.getVgroups(any())
        ).thenReturn(
            response.toMono()
        )
    }

    private fun mockGetAccount(promiseCredit: BigDecimal? = null, email: String? = null) {
        whenever(
            billingWebClient.getAccount(any())
        ).thenReturn(
            GetAccountResponse(
                ret = GetAccountRet(
                    agreements = listOf(
                        AccountAgreement(
                            promiseCredit = promiseCredit
                        )
                    ),
                    account = Account(
                        email = email
                    )
                )
            ).toMono()
        )
    }

    private fun mockGetRecommendedPayment(defaultPayment: BigDecimal, actualPayment: BigDecimal) {
        whenever(
            billingWebClient.getRecommendedPayment(any())
        ).thenReturn(
            GetRecommendedPaymentResponse(amount = defaultPayment).toMono()
        ).thenReturn(
            GetRecommendedPaymentResponse(amount = actualPayment).toMono()
        )
    }

    private fun StateDto.assertOptions(expectedAgreement: AgreementDto): StateDto {
        val options = this.options as AgreementOptions
        options.agreement.let { a ->
            assertThat(a).isNotNull
            assertThat(a!!.agreementId).isEqualTo(expectedAgreement.agreementId)
            assertThat(a.agreementNumber).isEqualTo(expectedAgreement.agreementNumber)
            assertThat(a.address).isEqualTo(expectedAgreement.address)
        }
        return this
    }
}