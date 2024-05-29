package su.vshk.billing.bot.dialog

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import reactor.kotlin.test.test
import su.vshk.billing.bot.dao.model.Command
import su.vshk.billing.bot.dialog.dto.StateDto
import su.vshk.billing.bot.dialog.option.GenericAvailableOptions
import su.vshk.billing.bot.dialog.option.PaymentHistoryAvailableOptions
import su.vshk.billing.bot.dialog.option.PaymentHistoryOptions
import su.vshk.billing.bot.dialog.option.PaymentHistoryPeriod

class PaymentHistoryDialogTest: BaseDialogTest() {

    private val command = Command.PAYMENT_HISTORY

    @Test
    fun testPaymentHistoryOk() {
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
                    .assertResponseTextContains("За какой период времени Вы хотите увидеть список платежей ?")
                    .assertButtonLabelsContains("за 1 месяц", "за 3 месяца", "за 6 месяцев", "⬅ Вернуться в главное меню")
                    .assertButtonCallbackDataContains("1", "3", "6", "/cancel")

                true
            }
            .verifyComplete()

        assertDialogContainsUser()

        dialogProcessor
            .processOption(
                request = createRequest(input = PaymentHistoryAvailableOptions.PERIOD_ONE_MONTH)
            )
            .test()
            .expectNextMatches {
                it.state
                    .assertFlags(isFinishedExpected = true, isCanceledExpected = false)
                    .assertOptions(expectedPeriod = PaymentHistoryPeriod.ONE_MONTH)
                    .assertResponseMessageIsNull()
                true
            }
            .verifyComplete()

        assertDialogDoesNotContainUser()
    }

    @Test
    fun testPaymentHistoryCancel() {
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
                    .assertResponseTextContains("За какой период времени Вы хотите увидеть список платежей ?")
                    .assertButtonLabelsContains("за 1 месяц", "за 3 месяца", "за 6 месяцев", "⬅ Вернуться в главное меню")
                    .assertButtonCallbackDataContains("1", "3", "6", "/cancel")

                true
            }
            .verifyComplete()

        assertDialogContainsUser()

        dialogProcessor
            .processOption(
                request = createRequest(input = GenericAvailableOptions.CANCEL)
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

    private fun StateDto.assertOptions(expectedPeriod: String): StateDto {
        val options = this.options as PaymentHistoryOptions
        assertThat(options.period).isEqualTo(expectedPeriod)
        return this
    }
}