package su.vshk.billing.bot.dialog

import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import su.vshk.billing.bot.dao.model.Command
import su.vshk.billing.bot.dao.model.UserEntity
import su.vshk.billing.bot.dialog.dto.DialogState
import su.vshk.billing.bot.dialog.dto.StateDto
import su.vshk.billing.bot.dialog.dto.UserDialogState
import su.vshk.billing.bot.dialog.dto.UserStateDto
import su.vshk.billing.bot.dialog.option.GenericAvailableOptions
import su.vshk.billing.bot.dialog.transformer.DialogStateTransformer
import su.vshk.billing.bot.message.dto.RequestMessageItem
import su.vshk.billing.bot.message.dto.ResponseMessageItem
import su.vshk.billing.bot.message.response.CommonMessageService
import java.util.concurrent.ConcurrentHashMap

@Component
class DialogProcessor(
    private val dialogTransformers: List<DialogStateTransformer>,
    private val commonMessageService: CommonMessageService
) {
    private val userDialogStates = ConcurrentHashMap<Long, UserDialogState>()

    /**
     * Получает команду, для которой пользователь находится в диалоге.
     *
     * @param telegramId telegramId пользователя
     * @return команда
     */
    fun getCommand(telegramId: Long): Command? =
        userDialogStates[telegramId]?.state?.command

    /**
     * Проверяет, находится ли пользователь в диалоге.
     *
     * @param telegramId telegramId пользователя
     * @return true - если пользователь в диалоге, false - в противном случае
     */
    fun contains(telegramId: Long): Boolean =
        userDialogStates.containsKey(telegramId)

    /**
     * Получает последнее сообщение в диалоге.
     *
     * @param telegramId telegramId пользователя
     * @return сообщение, которое отправилось пользователю
     */
    fun getLastResponseMessageItemContent(telegramId: Long): ResponseMessageItem.Content? =
        userDialogStates[telegramId]?.state?.response?.item?.content

    /**
     * Инициализирует диалог для пользователя.
     *
     * @param request запрос
     * @param user пользователь
     * @param command комманда
     * @return dto диалога
     */
    fun startDialog(request: RequestMessageItem, user: UserEntity, command: Command): Mono<UserStateDto> =
        Mono
            .defer {
                findDialogTransformer(command)
                    .initializeState(request, user)
                    .map { resolveResponse(user, it) }
            }
            .onErrorResume {
                userDialogStates.remove(user.telegramId)
                throw it
            }

    /**
     * Обработка опции.
     *
     * @param request запрос
     * @return dto диалога
     */
    fun processOption(request: RequestMessageItem): Mono<UserStateDto> =
        Mono
            .defer {
                val (user, dialogState) = unboxDialogState(request.chatId)
                if (request.input == GenericAvailableOptions.CANCEL) {
                    resolveResponse(user, dialogState.cancel(commonMessageService.showMainMenu())).toMono()
                } else {
                    findDialogTransformer(dialogState.command)
                        .processOption(request, user, dialogState)
                        .map { resolveResponse(user, it) }
                }
            }
            .onErrorResume {
                userDialogStates.remove(request.chatId)
                throw it
            }

    private fun unboxDialogState(telegramId: Long): Pair<UserEntity, DialogState> {
        val userDialogState = userDialogStates[telegramId]
        return Pair(userDialogState?.user!!, userDialogState.state!!)
    }

    private fun resolveResponse(user: UserEntity, state: DialogState): UserStateDto {
        val messageItem = state.response.item
        val meta = state.response.meta

        return when {
            meta.finish -> {
                userDialogStates.remove(user.telegramId)
                UserStateDto(user, StateDto.createFinishState(state.command, state.options))
            }

            meta.cancel -> {
                userDialogStates.remove(user.telegramId)
                UserStateDto(user, StateDto.createCancelState(messageItem!!))
            }

            meta.next -> {
                userDialogStates[user.telegramId] = UserDialogState(user, state)
                UserStateDto(user, StateDto.createNextState(messageItem!!))
            }

            else ->
                throw IllegalStateException("unexpected dialog state meta $meta")
        }
    }

    private fun findDialogTransformer(command: Command): DialogStateTransformer =
        dialogTransformers.find { it.getCommand() == command }
            ?: throw RuntimeException("could not find dialog state transformer for command ${command.value}")
}