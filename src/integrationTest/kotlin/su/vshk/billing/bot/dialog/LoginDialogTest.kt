package su.vshk.billing.bot.dialog

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import reactor.kotlin.test.test
import su.vshk.billing.bot.dao.model.Command
import su.vshk.billing.bot.dao.model.UserEntity
import su.vshk.billing.bot.dialog.dto.StateDto
import su.vshk.billing.bot.dialog.option.LoginOptions

class LoginDialogTest: BaseDialogTest() {

    private val command = Command.LOGIN

    @Test
    fun testStartOk() {
        dialogProcessor
            .startDialog(
                request = createRequest(
                    input = command.value,
                    messageId = 1
                ),
                user = createUnregisteredUser(),
                command = Command.LOGIN
            )
            .test()
            .expectNextMatches {
                it.state
                    .assertFlags(isFinishedExpected = false, isCanceledExpected = false)
                    .assertResponseTextContains("Добро пожаловать!", "Введите логин:")
                true
            }
            .verifyComplete()

        assertDialogContainsUser()

        dialogProcessor
            .processOption(
                request = createRequest(
                    input = "user",
                    messageId = 2
                )
            )
            .test()
            .expectNextMatches {
                it.state
                    .assertFlags(isFinishedExpected = false, isCanceledExpected = false)
                    .assertResponseTextContains("Введите пароль:")
                true
            }
            .verifyComplete()

        assertDialogContainsUser()

        dialogProcessor
            .processOption(
                request = createRequest(
                    input = "1234",
                    messageId = 3
                )
            )
            .test()
            .expectNextMatches {
                it.state
                    .assertFlags(isFinishedExpected = true, isCanceledExpected = false)
                    .assertOptions(expectedLogin = "user", expectedPassword = "1234")
                    .assertResponseMessageIsNull()
                true
            }
            .verifyComplete()

        assertDialogDoesNotContainUser()
    }

    private fun createUnregisteredUser() =
        UserEntity(telegramId = telegramId)

    private fun StateDto.assertOptions(expectedLogin: String, expectedPassword: String): StateDto {
        val options = this.options as LoginOptions
        assertThat(options.login).isEqualTo(expectedLogin)
        assertThat(options.password).isEqualTo(expectedPassword)
        return this
    }
}