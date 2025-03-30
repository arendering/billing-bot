package su.vshk.billing.bot.service.executor

import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import su.vshk.billing.bot.dao.model.Command
import su.vshk.billing.bot.dao.model.UserEntity
import su.vshk.billing.bot.message.dto.RequestMessageItem
import su.vshk.billing.bot.message.dto.ResponseMessageItem
import su.vshk.billing.bot.message.response.ContactsMessageService
import su.vshk.billing.bot.util.getLogger

@Service
class ContactsExecutor(
    private val contactsMessageService: ContactsMessageService
): CommandExecutor {

    private val logger = getLogger()

    override fun getCommand(): Command =
        Command.CONTACTS

    override fun execute(request: RequestMessageItem, user: UserEntity?, options: Any?): Mono<ResponseMessageItem> =
        Mono.fromCallable {
            logger.debug("Try to execute command '${getCommand().value}'")
            contactsMessageService.showContacts()
        }
}