package su.vshk.billing.bot.service.executor

import reactor.core.publisher.Mono
import su.vshk.billing.bot.dao.model.Command
import su.vshk.billing.bot.dao.model.UserEntity
import su.vshk.billing.bot.message.dto.ResponseMessageItem

//TODO: сделать options generic'ом
interface CommandExecutor {
    /**
     * Команда, для которой работает executor
     */
    fun getCommand(): Command

    /**
     * Выполняет команду.
     *
     * @param user пользователь
     * @param options опции команды
     */
    fun execute(user: UserEntity, options: Any? = null): Mono<ResponseMessageItem>
}