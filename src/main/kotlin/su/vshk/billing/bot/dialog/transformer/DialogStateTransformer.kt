package su.vshk.billing.bot.dialog.transformer

import su.vshk.billing.bot.dao.model.UserEntity
import su.vshk.billing.bot.dialog.dto.DialogState
import reactor.core.publisher.Mono
import su.vshk.billing.bot.dao.model.Command

interface DialogStateTransformer {

    /**
     * Команда для которой работает трансформер
     */
    fun getCommand(): Command

    /**
     * Инициализирует предварительный диалог, до ввода каких-либо опций от пользователя.
     *
     * @param user пользователь
     * @return диалог
     */
    fun initializePreState(user: UserEntity): Mono<DialogState>

    /**
     * Инициализирует данные, необходимые для выполнения шага.
     *
     * @param user пользователь
     * @param state состояние диалога
     * @return состояние диалога с инициализированными данными для следующего шага
     */
    fun initializeStepData(user: UserEntity, state: DialogState): Mono<DialogState> = Mono.fromCallable { state }

    /**
     * Делает проверки для текущего шага.
     *
     * @param user пользователь
     * @param state состояние диалога
     * @return true - если проверки успешны, false - в противном случае
     */
    fun isValidStep(user: UserEntity, state: DialogState): Boolean = true

    /**
     * Проверяет, валидно ли введенное пользователем значение опции.
     *
     * @param user пользователь
     * @param state состояние диалога
     * @param option опция
     * @return true - если значение опции валидно, false - в противном случае
     */
    fun isValidOption(user: UserEntity, state: DialogState, option: String): Boolean = true

    /**
     * Добавляет значение опции к диалогу.
     *
     * @param user пользователь
     * @param state диалог
     * @param option опция
     * @return диалог
     */
    fun addOption(user: UserEntity, state: DialogState, option: String): DialogState

    /**
     * Выполняет инкремент шага диалога.
     *
     * @param option опция
     * @param state диалог
     * @return диалог
     */
    fun incrementStep(option: String, state: DialogState): DialogState

    /**
     * Трансформирует успешное ответное сообщение.
     */
    fun transformMessage(state: DialogState): DialogState = state

    /**
     * Трансформирует ответное сообщение для некорректного шага.
     */
    fun transformInvalidStepMessage(state: DialogState): DialogState = state

    /**
     * Трансформирует ответное сообщение для некорректной опции.
     */
    fun transformInvalidOptionStepMessage(state: DialogState): DialogState = state
}