package su.vshk.billing.bot.dialog.dto

import su.vshk.billing.bot.dao.model.Command
import su.vshk.billing.bot.message.dto.ResponseMessageItem

data class StateDto(
    val isFinished: Boolean,
    val isCancelled: Boolean,
    val command: Command? = null,
    val options: Any? = null,
    val responseMessageItem: ResponseMessageItem? = null
) {
    companion object {
        /**
         * Отмена диалога.
         */
        fun createCancelState(message: ResponseMessageItem) =
            StateDto(
                isFinished = false,
                isCancelled = true,
                responseMessageItem = message
            )

        /**
         * Успешное завершение диалога для последующего процессинга.
         */
        fun createFinishState(command: Command, options: Any?) =
            StateDto(
                isFinished = true,
                isCancelled = false,
                command = command,
                options = options,
            )

        /**
         * Дальнейшее получение пользовательского ввода.
         */
        fun createNextState(message: ResponseMessageItem) =
            StateDto(
                isFinished = false,
                isCancelled = false,
                responseMessageItem = message
            )
    }
}