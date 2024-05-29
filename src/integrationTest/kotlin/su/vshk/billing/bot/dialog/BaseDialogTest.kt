package su.vshk.billing.bot.dialog

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import su.vshk.billing.bot.dao.model.UserEntity
import su.vshk.billing.bot.dialog.dto.StateDto
import su.vshk.billing.bot.message.dto.RequestMessageItem
import su.vshk.billing.bot.web.client.BillingWebClient

@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class BaseDialogTest {

    @Autowired
    protected lateinit var dialogProcessor: DialogProcessor

    @MockBean
    protected lateinit var billingWebClient: BillingWebClient

    protected val telegramId = 42L

    protected fun createRequest(input: String, messageId: Int = 13) =
        RequestMessageItem(isTextUpdate = false, isButtonUpdate = true, chatId = telegramId, input = input, messageId = messageId)

    protected fun createUser() =
        UserEntity(telegramId = telegramId, userId = 90, login = "user", agreementId = 80)

    protected fun StateDto.assertFlags(isFinishedExpected: Boolean, isCanceledExpected: Boolean): StateDto {
        assertThat(this.isFinished).isEqualTo(isFinishedExpected)
        assertThat(this.isCancelled).isEqualTo(isCanceledExpected)
        return this
    }

    protected fun StateDto.assertResponseTextContains(vararg expected: String): StateDto {
        assertThat(this.responseMessageItem?.content?.text).contains(*expected)
        return this
    }

    protected fun StateDto.assertResponseMessageIsNull(): StateDto {
        assertThat(this.responseMessageItem).isNull()
        return this
    }

    protected fun StateDto.assertButtonLabelsContains(vararg expected: String): StateDto {
        assertThat(
            this.responseMessageItem?.content?.buttons?.flatten()?.map { it.label }
        ).contains(*expected)

        return this
    }

    protected fun StateDto.assertButtonCallbackDataContains(vararg expected: String): StateDto {
        assertThat(
            this.responseMessageItem?.content?.buttons?.flatten()?.map { it.callbackData }
        ).contains(*expected)

        return this
    }

    protected fun assertDialogContainsUser() {
        assertThat(dialogProcessor.contains(telegramId)).isTrue()
    }

    protected fun assertDialogDoesNotContainUser() {
        assertThat(dialogProcessor.contains(telegramId)).isFalse()
    }

}