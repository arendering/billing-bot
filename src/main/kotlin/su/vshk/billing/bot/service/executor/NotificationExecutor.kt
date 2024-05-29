package su.vshk.billing.bot.service.executor

import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import su.vshk.billing.bot.dao.model.Command
import su.vshk.billing.bot.dao.model.PaymentNotificationEntity
import su.vshk.billing.bot.dao.model.PaymentNotificationType
import su.vshk.billing.bot.dao.model.UserEntity
import su.vshk.billing.bot.dao.service.PaymentNotificationDaoService
import su.vshk.billing.bot.dialog.option.NotificationAvailableOptions
import su.vshk.billing.bot.dialog.option.NotificationOptions
import su.vshk.billing.bot.message.dto.ResponseMessageItem
import su.vshk.billing.bot.message.response.NotificationMessageService
import su.vshk.billing.bot.util.debugTraceId
import su.vshk.billing.bot.util.getLogger

@Service
class NotificationExecutor(
    private val paymentNotificationDaoService: PaymentNotificationDaoService,
    private val notificationMessageService: NotificationMessageService
): CommandExecutor {

    private val logger = getLogger()

    override fun getCommand(): Command =
        Command.NOTIFICATION

    override fun execute(user: UserEntity, options: Any?): Mono<ResponseMessageItem> =
        Mono.deferContextual { context ->
            options as NotificationOptions
            logger.debugTraceId(context, "try to execute command '${getCommand().value}' with options: $options")

            when (val switch = options.switch) {
                NotificationAvailableOptions.ENABLE_FOR_SINGLE_AGREEMENT ->
                    Mono
                        .defer {
                            val entity = PaymentNotificationEntity(telegramId = user.telegramId, notificationType = PaymentNotificationType.SINGLE)
                            paymentNotificationDaoService.save(entity)
                        }
                        .map { notificationMessageService.singleAgreementEnabled() }

                NotificationAvailableOptions.ENABLE_FOR_ALL_AGREEMENTS ->
                    Mono
                        .defer {
                            val entity = PaymentNotificationEntity(telegramId = user.telegramId, notificationType = PaymentNotificationType.ALL)
                            paymentNotificationDaoService.save(entity)
                        }
                        .map { notificationMessageService.allAgreementsEnabled() }

                NotificationAvailableOptions.ENABLE ->
                    Mono
                        .defer {
                            val entity = PaymentNotificationEntity(telegramId = user.telegramId, notificationType = PaymentNotificationType.SINGLE)
                            paymentNotificationDaoService.save(entity)
                        }
                        .map { notificationMessageService.enabled() }


                NotificationAvailableOptions.DISABLE ->
                    Mono
                        .defer { paymentNotificationDaoService.deleteById(user.telegramId) }
                        .map { notificationMessageService.disabled() }

                else -> throw RuntimeException("unsupported switch option '$switch'")
            }
        }
}