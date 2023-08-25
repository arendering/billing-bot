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
import su.vshk.billing.bot.message.ResponseMessageService
import java.util.concurrent.ConcurrentHashMap

@Component
class DialogProcessor(
    private val dialogTransformers: List<DialogStateTransformer>,
    private val responseMessageService: ResponseMessageService
) {
    private val userDialogStates = ConcurrentHashMap<Long, UserDialogState>()

    /**
     * Инициализирует диалог для пользователя.
     *
     * @param user пользователь
     * @param command команда
     * @return dto диалога
     */
    fun startDialog(user: UserEntity, command: Command): Mono<UserStateDto> =
        Mono.defer {
            val transformer = findDialogTransformer(command)
            transformer.initializePreState(user)
                .flatMap { transformer.initializeStepData(user, it) }
                .map { runStepValidation(user, it) }
                .map { UserStateDto(user, it) }
        }

    /**
     * Проверяет, находится ли пользователь в диалоге.
     *
     * @param telegramId telegramId пользователя
     * @return true - если пользователь в диалоге, false - в противном случае
     */
    fun contains(telegramId: Long): Boolean =
        userDialogStates.containsKey(telegramId)

    fun isLoginDialog(telegramId: Long): Boolean =
        userDialogStates[telegramId]?.state?.command == Command.START

    /**
     * Обработка опции.
     *
     * @param telegramId telegramId пользователя
     * @param option опция
     * @return dto диалога
     */
    fun processOption(telegramId: Long, option: String): Mono<UserStateDto> =
        Mono.defer {
            val (user, dialogState) = unboxDialogState(telegramId)
            if (option == GenericAvailableOptions.CANCEL) {
                userDialogStates.remove(telegramId)
                UserStateDto(
                    user,
                    StateDto.createCancelState(responseMessageService.mainMenuMessage())
                ).toMono()
            } else {
                doProcessOption(user, option, dialogState)
                    .map { UserStateDto(user, it) }
            }
        }
            .onErrorResume {
                userDialogStates.remove(telegramId)
                throw it
            }

    private fun unboxDialogState(telegramId: Long): Pair<UserEntity, DialogState> {
        val userDialogState = userDialogStates[telegramId]
        return Pair(userDialogState?.user!!, userDialogState.state!!)
    }

    private fun doProcessOption(user: UserEntity, option: String, state: DialogState): Mono<StateDto> =
        Mono.defer {
            val transformer = findDialogTransformer(state.command!!)
            if (transformer.isValidOption(user, state, option)) {
                transformer.addOption(user, state, option)
                    .let { transformer.incrementStep(option, it) }
                    .let { runPostIncrement(user, it) }
            } else {
                val updatedState = transformer.transformInvalidOptionStepMessage(state)
                StateDto.createGetNextUserInputState(updatedState.messages?.invalidOptionMessage!!).toMono()
            }
        }

    private fun runPostIncrement(user: UserEntity, state: DialogState): Mono<StateDto> =
        Mono.defer {
            val transformer = findDialogTransformer(state.command!!)
            if (state.isDialogEnds()) {
                userDialogStates.remove(user.telegramId)
                StateDto.createFinishState(command = state.command, options = state.options).toMono()
            } else {
                transformer.initializeStepData(user, state)
                    .map { runStepValidation(user, it) }
            }
        }

    private fun runStepValidation(user: UserEntity, state: DialogState): StateDto {
        val transformer = findDialogTransformer(state.command!!)

        return if (transformer.isValidStep(user, state)) {
            transformer.transformMessage(state).let {
                userDialogStates[user.telegramId] = UserDialogState(user, it)
                StateDto.createGetNextUserInputState(it.messages?.message!!)
            }
        } else {
            transformer.transformInvalidStepMessage(state).let {
                if (userDialogStates.containsKey(user.telegramId)) {
                    userDialogStates.remove(user.telegramId)
                }
                StateDto.createCancelState(it.messages?.invalidStepMessage!!)
            }
        }
    }

    private fun findDialogTransformer(command: Command): DialogStateTransformer =
        dialogTransformers.find { it.getCommand() == command }
            ?: throw RuntimeException("could not find dialog state transformer for command ${command.value}")
}