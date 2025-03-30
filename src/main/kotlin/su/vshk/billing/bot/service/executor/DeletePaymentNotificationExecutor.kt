package su.vshk.billing.bot.service.executor

import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import su.vshk.billing.bot.dao.model.Command
import su.vshk.billing.bot.dao.model.UserEntity
import su.vshk.billing.bot.message.dto.RequestMessageItem
import su.vshk.billing.bot.message.dto.ResponseMessageItem
import su.vshk.billing.bot.service.PaymentNotificationService
import su.vshk.billing.bot.util.getLogger

@Service
class DeletePaymentNotificationExecutor(
    private val paymentNotificationService: PaymentNotificationService
): CommandExecutor {

    private val logger = getLogger()

    override fun getCommand(): Command =
        Command.DELETE_PAYMENT_NOTIFICATION

    override fun execute(request: RequestMessageItem, user: UserEntity?, options: Any?): Mono<ResponseMessageItem> =
        Mono.defer {
            logger.debug("Try to execute command '${getCommand().value}'")
            paymentNotificationService.deletePaymentNotification(request)
        }
}