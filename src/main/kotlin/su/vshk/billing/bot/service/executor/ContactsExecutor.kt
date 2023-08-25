package su.vshk.billing.bot.service.executor

import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import su.vshk.billing.bot.dao.model.Command
import su.vshk.billing.bot.dao.model.UserEntity
import su.vshk.billing.bot.message.ResponseMessageService
import su.vshk.billing.bot.message.dto.ResponseMessageItem
import su.vshk.billing.bot.util.debugTraceId
import su.vshk.billing.bot.util.getLogger

@Service
class ContactsExecutor(
    private val responseMessageService: ResponseMessageService
): CommandExecutor {
    companion object {
        private val log = getLogger()
    }

    override fun getCommand(): Command =
        Command.CONTACTS

    override fun execute(user: UserEntity, options: Any?): Mono<ResponseMessageItem> =
        Mono.deferContextual { context ->
            log.debugTraceId(context, "try to execute command '${getCommand().value}'")
            responseMessageService.contactsMessage().toMono()
        }
}