package su.vshk.billing.bot.dialog.dto

import su.vshk.billing.bot.dao.model.Command
import su.vshk.billing.bot.message.dto.ResponseMessageItem

data class DialogState(
    /**
     * Команда.
     */
    val command: Command,

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
     * Ответ пользователю.
     */
    val response: Response
) {
    /**
     * Получает текущий шаг диалога.
     */
    fun currentStep(): String = steps[stepIndex]

    /**
     * Переходит на следующий шаг диалога.
     */
    fun incrementStep(options: Any?, responseMessageItem: ResponseMessageItem): DialogState =
        this.copy(
            options = options,
            stepIndex = this.stepIndex + 1,
            response = Response.next(responseMessageItem)
        )

    /**
     * Остаться на текущем шаге диалога.
     */
    fun stayCurrentStep(responseMessageItem: ResponseMessageItem): DialogState =
        this.copy(response = Response.next(responseMessageItem))

    /**
     * Завершить успешно диалог.
     */
    fun finish(options: Any?): DialogState =
        this.copy(
            options = options,
            response = Response.finish()
        )

    /**
     * Отменить диалог.
     */
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