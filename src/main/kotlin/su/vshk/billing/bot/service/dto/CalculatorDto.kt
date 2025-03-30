package su.vshk.billing.bot.service.dto

import su.vshk.billing.bot.dao.model.Command
import su.vshk.billing.bot.message.dto.ResponseMessageItem

data class CalculatorStateDto(
    /**
     * Текущая сумма.
     */
    val amount: Int,
    /**
     * Команда, для которой вводится сумма.
     */
    val command: Command
)

data class CalculatorResponseDto(
    /**
     * Флаг для ввода следующей опции на шаге ввода суммы.
     */
    val next: Boolean = false,
    /**
     * Флаг для отмены команды.
     */
    val cancel: Boolean = false,
    /**
     * Флаг для окончания ввода на шаге ввода суммы.
     */
    val finish: Boolean = false,
    /**
     * Ответ пользователю.
     */
    val response: ResponseMessageItem? = null,
    /**
     * Итоговая сумма.
     */
    val amount: Int? = null
) {
    companion object {
        fun next(response: ResponseMessageItem) = CalculatorResponseDto(next = true, response = response)
        fun cancel(response: ResponseMessageItem) = CalculatorResponseDto(cancel = true, response = response)
        fun finish(amount: Int) = CalculatorResponseDto(finish = true, amount = amount)
    }
}
