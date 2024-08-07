package su.vshk.billing.bot.service

import org.springframework.stereotype.Service
import su.vshk.billing.bot.dao.model.CalculatorButton
import su.vshk.billing.bot.dao.model.Command
import su.vshk.billing.bot.message.dto.ResponseMessageItem
import su.vshk.billing.bot.message.response.CalculatorMessageService
import su.vshk.billing.bot.service.dto.CalculatorResponseDto
import su.vshk.billing.bot.service.dto.CalculatorStateDto
import java.util.concurrent.ConcurrentHashMap

@Service
class CalculatorService(
    private val calculatorMessageService: CalculatorMessageService
) {

    private val supportedCommands = listOf(Command.PROMISE_PAYMENT, Command.YOOKASSA_PAYMENT)
    private val emptyAmount = 0

    /**
     * Хранит текущее значение суммы платежа.
     * Key - telegramId, value - состояние суммы для пользователя
     */
    private val amountCache = ConcurrentHashMap<Long, CalculatorStateDto>()

    /**
     * Получает максимальную сумму для команды.
     */
    fun getMaxLimit(command: Command): Int =
        when (supportedCommands.find { it == command }) {
            Command.PROMISE_PAYMENT -> 1_500
            Command.YOOKASSA_PAYMENT -> 100_000
            else -> throw RuntimeException("could not resolve max limit for command ${command.value}")
        }

    /**
     * Инициализация состояния калькулятора.
     */
    fun initialize(telegramId: Long, amount: Int, command: Command): ResponseMessageItem {
        if (command !in supportedCommands) {
            throw RuntimeException("unsupported calculator command ${command.value}")
        }

        amountCache[telegramId] = CalculatorStateDto(amount = amount, command = command)
        return calculatorMessageService.showCalculator(amount)
    }

    /**
     * Обработка ввода с калькулятора.
     */
    fun processOption(telegramId: Long, option: String): CalculatorResponseDto =
        try {
            amountCache[telegramId]
                ?: throw RuntimeException("unable to get amount from calculator cache for user '$telegramId'")

            when (option) {
                CalculatorButton.CANCEL_AMOUNT_STEP -> cancelAmountStep(telegramId)
                CalculatorButton.ENTER -> enterAmount(telegramId)
                CalculatorButton.ERASE -> eraseLastDigit(telegramId)
                CalculatorButton.CLEAR -> clearAmount(telegramId)
                in CalculatorButton.DIGITS -> increaseAmount(telegramId, option)
                else -> throw RuntimeException("calculator unexpected option '$option' for user '$telegramId'")
            }
        } catch (th: Throwable) {
            amountCache.remove(telegramId)
            throw th
        }

    private fun cancelAmountStep(telegramId: Long): CalculatorResponseDto {
        amountCache.remove(telegramId)
        return CalculatorResponseDto.cancel(calculatorMessageService.showMainMenu())
    }

    private fun enterAmount(telegramId: Long): CalculatorResponseDto {
        val amount = amountCache[telegramId]!!.amount

        return if (amount == emptyAmount) {
            CalculatorResponseDto.next(calculatorMessageService.showEmptyAmountWarningCalculator())
        } else {
            amountCache.remove(telegramId)
            CalculatorResponseDto.finish(amount)
        }
    }

    private fun eraseLastDigit(telegramId: Long): CalculatorResponseDto {
        val state = amountCache[telegramId]!!

        return if (state.amount == emptyAmount) {
            CalculatorResponseDto.next(calculatorMessageService.showEmptyAmountWarningCalculator())
        } else {
            val updatedAmount = state.amount
                .toString()
                .dropLast(1)
                .let { erased ->
                    if (erased.isEmpty()) {
                        emptyAmount
                    } else {
                        erased.toInt()
                    }
                }

            amountCache[telegramId] = state.copy(amount = updatedAmount)
            return CalculatorResponseDto.next(calculatorMessageService.showCalculator(updatedAmount))
        }
    }

    private fun clearAmount(telegramId: Long): CalculatorResponseDto {
        val state = amountCache[telegramId]!!

        amountCache[telegramId] = state.copy(amount = emptyAmount)

        val responseMessage =
            if (state.amount == emptyAmount) {
                calculatorMessageService.showEmptyAmountWarningCalculator()
            } else {
                calculatorMessageService.showCalculator(emptyAmount)
            }

        return CalculatorResponseDto.next(responseMessage)
    }

    private fun increaseAmount(telegramId: Long, digit: String): CalculatorResponseDto {
        val state = amountCache[telegramId]!!
        val updatedAmount = if (state.amount == emptyAmount) digit.toInt() else "${state.amount}$digit".toInt()
        val maxLimit = getMaxLimit(state.command)

        return when {
            updatedAmount == emptyAmount ->
                CalculatorResponseDto.next(calculatorMessageService.showEmptyAmountWarningCalculator())

            updatedAmount > maxLimit ->
                CalculatorResponseDto.next(calculatorMessageService.showMaxAmountWarningCalculator(state.amount, maxLimit))

            else -> {
                amountCache[telegramId] = state.copy(amount = updatedAmount)
                CalculatorResponseDto.next(calculatorMessageService.showCalculator(updatedAmount))
            }
        }
    }
}