package su.vshk.billing.bot.scheduler

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.scheduler.Schedulers
import reactor.kotlin.core.publisher.toMono
import su.vshk.billing.bot.Bot
import su.vshk.billing.bot.config.BotProperties
import su.vshk.billing.bot.dao.model.EnabledNotificationUserDto
import su.vshk.billing.bot.dao.service.UserDaoService
import su.vshk.billing.bot.message.response.NotificationMessageService
import su.vshk.billing.bot.service.PaymentNotificationService
import su.vshk.billing.bot.util.getLogger
import su.vshk.billing.bot.util.runWithMdcContext
import java.time.Duration

@Service
class PaymentScheduler(
    private val bot: Bot,
    private val properties: BotProperties,
    private val userDaoService: UserDaoService,
    private val paymentNotificationService: PaymentNotificationService,
    private val notificationMessageService: NotificationMessageService
) {

    private val logger = getLogger()

    /**
     * Отправляет напоминание об оплате за 5 дней до конца месяца.
     */
    @Scheduled(cron = "\${bot.payment-notification.five-days-send-rule:0 0 13 L-5 * *}")
    fun sendFiveDaysPaymentNotification() {
        sendPaymentNotifications(PaymentSchedulerPeriod.FIVE_DAYS)
    }

    /**
     * Удаляет напоминания об оплате за 5 дней (через 32 часа после отправки).
     */
    @Scheduled(cron = "\${bot.payment-notification.five-days-delete-rule:0 0 21 L-4 * *}")
    fun deleteFiveDaysPaymentNotification() {
        deletePaymentNotifications()
    }

    /**
     * Отправляет напоминание об оплате за 1 день до конца месяца.
     */
    @Scheduled(cron = "\${bot.payment-notification.one-day-send-rule:0 0 13 L-1 * *}")
    fun sendOneDayPaymentNotification() {
        sendPaymentNotifications(PaymentSchedulerPeriod.ONE_DAY)
    }

    /**
     * Удаляет напоминания об оплате за 1 день (через 32 часа после отправки).
     */
    @Scheduled(cron = "\${bot.payment-notification.one-day-delete-rule:0 0 21 L * *}")
    fun deleteOneDayPaymentNotification() {
        deletePaymentNotifications()
    }

    private fun sendPaymentNotifications(daysToLast: Int) {
        userDaoService.findUsersEnabledNotification()
            .flatMapMany { Flux.fromIterable(it) }
            .delayElements(Duration.ofSeconds(properties.paymentNotification.billingRequestDelaySeconds))
            .flatMap({ dto -> runWithMdcContext(rx = doSendNotification(dto = dto, daysToLast = daysToLast)) }, 10)
            .collectList()
            .flatMap { paymentNotificationService.savePaymentNotificationMessages(it) }
            .doOnError { logger.error("Send payment notification error: ${it.stackTraceToString()}") }
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe()
    }

    private fun doSendNotification(dto: EnabledNotificationUserDto, daysToLast: Int) =
        paymentNotificationService.createPaymentNotification(userDto = dto, daysToLast = daysToLast)
            .flatMap {
                bot.sendResponse(chatId = dto.telegramId, responseMessageItem = it)
                    .onErrorResume { notificationMessageService.emptyMessage().toMono() }
            }
            .map { Pair(dto.telegramId, it) }

    private fun deletePaymentNotifications() {
        paymentNotificationService.deletePaymentNotifications()
            .flatMapMany { Flux.fromIterable(it) }
            .flatMap(
                { (telegramId, responseMessageItem) ->
                    runWithMdcContext(rx = bot.sendResponse(chatId = telegramId, responseMessageItem = responseMessageItem)) },
                10
            )
            .doOnError { logger.error("Delete payment notification error: ${it.stackTraceToString()}") }
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe()
    }
}