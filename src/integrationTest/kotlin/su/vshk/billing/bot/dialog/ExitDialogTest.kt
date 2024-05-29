package su.vshk.billing.bot.dialog

import org.junit.jupiter.api.Test
import reactor.kotlin.test.test
import su.vshk.billing.bot.dao.model.Command
import su.vshk.billing.bot.dialog.option.ExitAvailableOptions

class ExitDialogTest: BaseDialogTest() {

    private val command = Command.EXIT

    @Test
    fun testExitOk() {
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
                    .assertResponseTextContains("После выхода из системы функции бота станут недоступны. Вы уверены, что хотите отвязать устройство ?")
                    .assertButtonLabelsContains("Отвязать", "⬅ Вернуться в главное меню")
                    .assertButtonCallbackDataContains("/yes", "/cancel")
                true
            }
            .verifyComplete()

        assertDialogContainsUser()

        dialogProcessor
            .processOption(request = createRequest(input = ExitAvailableOptions.YES))
            .test()
            .expectNextMatches {
                it.state
                    .assertFlags(isFinishedExpected = true, isCanceledExpected = false)
                    .assertResponseMessageIsNull()
                true
            }
            .verifyComplete()

        assertDialogDoesNotContainUser()
    }
}