package su.vshk.billing.bot.service.executor

import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import su.vshk.billing.bot.dao.model.Command
import su.vshk.billing.bot.dao.model.UserEntity
import su.vshk.billing.bot.dialog.option.NotificationOptions
import su.vshk.billing.bot.message.ResponseMessageService
import su.vshk.billing.bot.message.dto.ResponseMessageItem
import su.vshk.billing.bot.dao.service.UserDaoService
import su.vshk.billing.bot.util.debugTraceId
import su.vshk.billing.bot.util.getLogger

@Service
class NotificationExecutor(
    private val responseMessageService: ResponseMessageService,
    private val userDaoService: UserDaoService
): CommandExecutor {

    companion object {
        private val log = getLogger()
    }

    override fun getCommand(): Command =
        Command.NOTIFICATION

    override fun execute(user: UserEntity, options: Any?): Mono<ResponseMessageItem> =
        Mono.deferContextual { context ->
            options as NotificationOptions
            log.debugTraceId(context, "try to execute command '${getCommand().value}' with options: ${options}")

            userDaoService.updateUser(user.copy(paymentNotificationEnabled = options.enable))
                .map {
                    if (options.enable!!) {
                        responseMessageService.notificationSuccessEnableMessage()
                    } else {
                        responseMessageService.notificationSuccessDisableMessage()
                    }
                }
        }
}