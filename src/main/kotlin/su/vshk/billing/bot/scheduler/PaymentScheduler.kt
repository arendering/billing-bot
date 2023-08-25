package su.vshk.billing.bot.scheduler

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.scheduler.Schedulers
import su.vshk.billing.bot.Bot
import su.vshk.billing.bot.config.BotProperties
import su.vshk.billing.bot.dao.service.UserDaoService
import su.vshk.billing.bot.service.PaymentNotificationService
import su.vshk.billing.bot.util.getLogger
import su.vshk.billing.bot.util.putTraceId
import java.time.Duration

@Service
class PaymentScheduler(
    private val bot: Bot,
    private val properties: BotProperties,
    private val userDaoService: UserDaoService,
    private val paymentNotificationService: PaymentNotificationService,
) {

    companion object {
        private val log = getLogger()
    }

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
        userDaoService.findByPaymentNotificationEnabledTrue()
            .flatMapMany { Flux.fromIterable(it) }
            .delayElements(Duration.ofSeconds(properties.paymentNotification.billingRequestDelaySeconds))
            .flatMap({ user ->
                paymentNotificationService.createPaymentNotification(user = user, daysToLast = daysToLast)
                    .flatMap { bot.sendResponse(chatId = user.telegramId, responseMessageItem = it) }
                    .map { Pair(user.telegramId, it) }
                    .putTraceId()
            }, 10 ) // значение concurrency выбрано произвольно
            .collectList()
            .flatMap { paymentNotificationService.savePaymentNotificationMessages(it) }
            .doOnError { log.error("error occurs while sending notifications: ${it.stackTraceToString()}") }
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe()
    }

    private fun deletePaymentNotifications() {
        paymentNotificationService.deletePaymentNotifications()
            .flatMapMany { Flux.fromIterable(it) }
            .flatMap({ (telegramId, responseMessageItem) ->
                bot.sendResponse(chatId = telegramId, responseMessageItem = responseMessageItem)
            }, 10 ) // значение concurrency выбрано произвольно
            .doOnError { log.error("error occurs while deleting notifications: ${it.stackTraceToString()}") }
            .putTraceId()
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe()
    }
}