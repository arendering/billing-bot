package su.vshk.billing.bot.dialog.transformer

import su.vshk.billing.bot.dao.model.UserEntity
import su.vshk.billing.bot.dialog.dto.DialogState
import reactor.core.publisher.Mono
import su.vshk.billing.bot.dao.model.Command
import su.vshk.billing.bot.message.dto.RequestMessageItem

interface DialogStateTransformer {

    /**
     * Команда для которой работает трансформер.
     */
    fun getCommand(): Command

    /**
     * Инициализирует предварительный диалог, до ввода каких-либо опций от пользователя.
     *
     * @param request запрос
     * @param user пользователь
     * @return состояние диалога
     */
    fun initializeState(request: RequestMessageItem, user: UserEntity): Mono<DialogState>

    /**
     * Обработка опции от пользователя.
     *
     * @param request запрос
     * @param user пользователь
     * @param state состояние диалога
     * @return состояние диалога с добавленной опцией
     */
    fun processOption(request: RequestMessageItem, user: UserEntity, state: DialogState): Mono<DialogState>
}