package su.vshk.billing.bot.service.executor

import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import su.vshk.billing.bot.dao.model.Command
import su.vshk.billing.bot.dao.model.UserEntity
import su.vshk.billing.bot.dao.service.PaymentNotificationDaoService
import su.vshk.billing.bot.dao.service.PaymentNotificationMessageDaoService
import su.vshk.billing.bot.dao.service.UserDaoService
import su.vshk.billing.bot.message.dto.RequestMessageItem
import su.vshk.billing.bot.message.dto.ResponseMessageItem
import su.vshk.billing.bot.message.response.LogoutMessageService
import su.vshk.billing.bot.service.CookieManager
import su.vshk.billing.bot.util.getLogger

@Service
class LogoutExecutor(
    private val userDaoService: UserDaoService,
    private val logoutMessageService: LogoutMessageService,
    private val paymentNotificationMessageDaoService: PaymentNotificationMessageDaoService,
    private val paymentNotificationDaoService: PaymentNotificationDaoService,
    private val cookieManager: CookieManager
): CommandExecutor {

    private val logger = getLogger()

    override fun getCommand(): Command =
        Command.EXIT

    //TODO: удалять данные из таблиц нужно в транзакции
    override fun execute(request: RequestMessageItem, user: UserEntity?, options: Any?): Mono<ResponseMessageItem> =
        Mono.defer {
            logger.debug("Try to execute command '${getCommand().value}'")
            userDaoService.deleteUser(user!!.telegramId)
                .flatMap { cookieManager.removeClientCookie(user.userId!!) }
                .flatMap { paymentNotificationDaoService.removeByIdSafe(user.telegramId) }
                .flatMap { paymentNotificationMessageDaoService.removeByIdSafe(user.telegramId) }
                .map {
                    val deleteMessageId = it.orElse(null)?.messageId
                    logoutMessageService.showSuccessLogout(deleteMessageId)
                }
        }
}