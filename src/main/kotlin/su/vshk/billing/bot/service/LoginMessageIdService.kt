package su.vshk.billing.bot.service

import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import su.vshk.billing.bot.message.dto.RequestMessageItem
import su.vshk.billing.bot.service.dto.LoginInputDto
import su.vshk.billing.bot.service.dto.LoginMessageDto
import java.util.concurrent.ConcurrentHashMap

/**
 * Сервис для управления идентификаторами пользовательских сообщений на этапе регистрации в боте.
 */
@Service
class LoginMessageIdService {
    private val messages: ConcurrentHashMap<Long, LoginInputDto> = ConcurrentHashMap()

    fun init(request: RequestMessageItem): Mono<Int> =
        Mono.fromCallable {
            messages[request.chatId] = LoginInputDto(
                isFirstMessageTextInput = request.isTextUpdate,
                isFirstMessageButtonInput = request.isButtonUpdate,
                messageIds = listOf(request.messageId)
            )

            request.messageId
        }

    fun add(telegramId: Long, messageId: Int): Mono<Int> =
        Mono.fromCallable {
            val updatedDto = messages[telegramId]
                ?.addMessageId(messageId)
                ?: throw RuntimeException("could not found message ids for user '$telegramId'")

            messages[telegramId] = updatedDto
            messageId
        }

    fun remove(telegramId: Long): Mono<LoginMessageDto> =
        Mono.fromCallable {
            val dto = messages.remove(telegramId) ?: throw RuntimeException("could not found message ids for user '$telegramId'")
            val messageIds = dto.messageIds

            when {
                dto.isFirstMessageTextInput ->
                    LoginMessageDto(
                        editMessageId = null,
                        deleteMessageIds = messageIds.asReversed()
                    )

                dto.isFirstMessageButtonInput ->
                    LoginMessageDto(
                        editMessageId = messageIds.first(),
                        deleteMessageIds = messageIds.asReversed().dropLast(1)
                    )

                else -> throw IllegalStateException("unreachable code")
            }
        }

    fun isEmpty(telegramId: Long): Boolean =
        messages[telegramId] == null
}