package su.vshk.billing.bot.dialog.dto

import su.vshk.billing.bot.dao.model.Command
import su.vshk.billing.bot.message.dto.ResponseMessageItem

data class DialogState(
    /**
     * Команда.
     */
    val command: Command,

    //TODO: сделать generic'ом
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
     * Ответ пользователю.
     */
    val response: Response
) {
    fun currentStep(): String = steps[stepIndex]

    fun incrementStep(options: Any?, stepData: Any? = null, responseMessageItem: ResponseMessageItem): DialogState =
        this.copy(
            options = options,
            stepData = stepData,
            stepIndex = this.stepIndex + 1,
            response = Response.next(responseMessageItem)
        )

    fun invalidOption(responseMessageItem: ResponseMessageItem): DialogState =
        this.copy(response = Response.next(responseMessageItem))

    fun updateStepData(stepData: Any?, responseMessageItem: ResponseMessageItem): DialogState =
        this.copy(
            stepData = stepData,
            response = Response.next(responseMessageItem)
        )

    fun finish(options: Any?): DialogState =
        this.copy(
            options = options,
            response = Response.finish()
        )

    fun cancel(responseMessageItem: ResponseMessageItem): DialogState =
        this.copy(response = Response.cancel(responseMessageItem))

    data class Response(
        val item: ResponseMessageItem? = null,
        val meta: Meta = Meta()
    ) {
        companion object {
            fun finish() = Response(meta = Meta(finish = true))
            fun cancel(item: ResponseMessageItem) = Response(item = item, meta = Meta(cancel = true))
            fun next(item: ResponseMessageItem) = Response(item = item, meta = Meta(next = true))
        }
    }

    data class Meta(
        /**
         * Диалог успешно закончен.
         */
        val finish: Boolean = false,

        /**
         * Диалог отменен.
         */
        val cancel: Boolean = false,

        /**
         * Ожидаем пользовательский ввод.
         */
        val next: Boolean = false
    )
}