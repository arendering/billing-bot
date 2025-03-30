package su.vshk.billing.bot.service.executor

import reactor.core.publisher.Mono
import su.vshk.billing.bot.dao.model.Command
import su.vshk.billing.bot.dao.model.UserEntity
import su.vshk.billing.bot.message.dto.RequestMessageItem
import su.vshk.billing.bot.message.dto.ResponseMessageItem

interface CommandExecutor {
    /**
     * Команда, для которой работает executor
     */
    fun getCommand(): Command

    /**
     * Выполняет команду.
     *
     * @param request запрос
     * @param user пользователь
     * @param options опции команды
     */
    fun execute(request: RequestMessageItem, user: UserEntity?, options: Any?): Mono<ResponseMessageItem>
}