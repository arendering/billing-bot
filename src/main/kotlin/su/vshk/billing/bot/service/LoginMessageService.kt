package su.vshk.billing.bot.service

import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import su.vshk.billing.bot.message.dto.RequestMessageItem
import su.vshk.billing.bot.service.dto.LoginInputDto
import su.vshk.billing.bot.service.dto.LoginMessageDto
import java.util.concurrent.ConcurrentHashMap

/**
 * Сервис для управления пользовательскими сообщениями на этапе регистрации в боте.
 */
@Service
class LoginMessageService {
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
            val dto = messages[telegramId]!!
            val updatedMessageIds = dto.messageIds.plus(messageId)
            val updatedDto = dto.copy(messageIds = updatedMessageIds)
            messages[telegramId] = updatedDto

            messageId
        }

    fun remove(telegramId: Long): Mono<LoginMessageDto> =
        Mono.fromCallable {
            val dto = messages.remove(telegramId)!!
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