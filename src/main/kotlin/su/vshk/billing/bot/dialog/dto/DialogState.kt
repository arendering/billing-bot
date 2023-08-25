package su.vshk.billing.bot.dialog.dto

import su.vshk.billing.bot.dao.model.Command
import su.vshk.billing.bot.message.dto.ResponseMessageItem

//TODO: сделать options и stepData generic'ами
data class DialogState(
    /**
     * Команда.
     */
    val command: Command? = null,

    /**
     * Опции, введенные пользователем.
     */
    val options: Any? = null,

    /**
     * Шаги для команды.
     */
    val steps: List<String> = emptyList(),

    /**
     * Индекс текущего шага.
     */
    val stepIndex: Int = 0,

    /**
     * Дополнительные данные, которые нужны для обработки опции на текущем шаге.
     */
    val stepData: Any? = null,

    /**
     * Ответные сообщения для пользователя.
     */
    val messages: MessageContainer? = null
) {
    fun isDialogEnds() = stepIndex == steps.size
    fun currentStep() = steps[stepIndex]

    data class MessageContainer(
        /**
         * Сообщение пользователю для случая, если все ок.
         */
        val message: ResponseMessageItem? = null,

        /**
         * Шаблон текста для случая, если все ок.
         */
        val templateText: String? = null,

        /**
         * Сообщение пользователю для случая, если проверки для текущего шага окончилась неудачей.
         */
        val invalidStepMessage: ResponseMessageItem? = null,

        /**
         * Шаблон текста для случая, если проверки для текущего шага окончилась неудачей.
         */
        val invalidStepTemplateText: String? = null,

        /**
         * Сообщение пользователю для случая, если проверка значения опции для текущего шага окончилась неудачей.
         */
        val invalidOptionMessage: ResponseMessageItem? = null,

        /**
         * Шаблон текста для случая, если проверка значения опции для текущего шага окончилась неудачей.
         */
        val invalidOptionTemplateText: String? = null
    )
}