package su.vshk.billing.bot.service.executor

import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import su.vshk.billing.bot.dao.model.Command
import su.vshk.billing.bot.dao.model.UserEntity
import su.vshk.billing.bot.dao.service.UserDaoService
import su.vshk.billing.bot.dialog.option.AgreementOptions
import su.vshk.billing.bot.message.dto.ResponseMessageItem
import su.vshk.billing.bot.message.response.AgreementMessageService
import su.vshk.billing.bot.util.debugTraceId
import su.vshk.billing.bot.util.getLogger

@Service
class AgreementsExecutor(
    private val userDaoService: UserDaoService,
    private val agreementMessageService: AgreementMessageService
): CommandExecutor {

    private val logger = getLogger()

    override fun getCommand(): Command =
        Command.AGREEMENTS

    override fun execute(user: UserEntity, options: Any?): Mono<ResponseMessageItem> =
        Mono.deferContextual { context ->
            options as AgreementOptions
            logger.debugTraceId(context, "try to execute command '${getCommand().value}' with options: $options")

            val updatedUser = user.copy(agreementId = options.agreement!!.agreementId)
            userDaoService.updateUser(updatedUser)
                .map { agreementMessageService.switchAgreement(options.agreement) }
        }
}