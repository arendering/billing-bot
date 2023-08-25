package su.vshk.billing.bot.service

import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import su.vshk.billing.bot.dao.model.PaymentNotificationMessageEntity
import su.vshk.billing.bot.dao.model.UserEntity
import su.vshk.billing.bot.dao.service.PaymentNotificationMessageDaoService
import su.vshk.billing.bot.message.ResponseMessageService
import su.vshk.billing.bot.message.dto.RequestMessageItem
import su.vshk.billing.bot.message.dto.ResponseMessageItem
import su.vshk.billing.bot.scheduler.PaymentSchedulerPeriod
import su.vshk.billing.bot.util.*
import java.math.BigDecimal

@Service
class PaymentNotificationService(
    private val infoService: InfoService,
    private val recommendedPaymentService: RecommendedPaymentService,
    private val responseMessageService: ResponseMessageService,
    private val paymentNotificationMessageDaoService: PaymentNotificationMessageDaoService
) {

    companion object {
        private val log = getLogger()
    }

    /**
     * Создает напоминание об оплате.
     */
    fun createPaymentNotification(user: UserEntity, daysToLast: Int): Mono<ResponseMessageItem> =
        Mono
            .deferContextual { context ->
                log.infoTraceId(context, "try to send notification, user ${user.telegramId}, daysToLast '$daysToLast'")
                getBalanceAndRecommendedPayment(user)
                    .map { (balance, actualRecommendedPayment) ->
                        if (AmountUtils.isZero(actualRecommendedPayment)) {
                            log.infoTraceId(context, "actualRecommendedPayment == 0, notification sending was skipped...")
                            responseMessageService.emptyMessage()
                        } else {
                            resolveMessage(
                                daysToLast = daysToLast,
                                balance = balance,
                                actualRecommendedPayment = actualRecommendedPayment
                            )
                        }
                    }
            }
            .onErrorResume {
                Mono.deferContextual { context ->
                    log.errorTraceId(context, "notification error: ${it.stackTraceToString()}")
                    responseMessageService.notifyErrorGroupMessage().toMono()
                }
            }

    /**
     * Сохраняет messageId отправленных напоминаний.
     */
    fun savePaymentNotificationMessages(responses: List<Pair<Long, ResponseMessageItem>>): Mono<List<PaymentNotificationMessageEntity>> =
        responses
            .filter { (_, responseMessageItem) -> responseMessageItem.meta.sendMessage.active }
            .map { (telegramId, responseMessageItem) ->
                PaymentNotificationMessageEntity(
                    telegramId = telegramId,
                    messageId = responseMessageItem.meta.sendMessage.messageId!!
                )
            }
            .let { paymentNotificationMessageDaoService.saveAll(it) }

    /**
     * Удаляет напоминание об оплате (пользователь нажал на кнопку).
     */
    fun deletePaymentNotification(request: RequestMessageItem): Mono<ResponseMessageItem> =
        paymentNotificationMessageDaoService.removeById(request.chatId)
            .map { responseMessageService.deleteMessage(request.messageId) }

    /**
     * Удаляет напоминания об оплате (по планировщику).
     */
    fun deletePaymentNotifications(): Mono<List<Pair<Long, ResponseMessageItem>>> =
        paymentNotificationMessageDaoService.removeAll()
            .map { entities ->
                entities.map { e ->
                    Pair(
                        e.telegramId,
                        responseMessageService.deleteMessage(e.messageId)
                    )
                }
            }

    private fun getBalanceAndRecommendedPayment(user: UserEntity): Mono<Pair<BigDecimal, BigDecimal>> =
        infoService.getBalance(user.uid!!)
            .flatMap { balance ->
                recommendedPaymentService.getActual(user.agrmId!!)
                    .map { actualRecommendedPayment ->
                        Pair(balance, actualRecommendedPayment)
                    }
            }

    private fun resolveMessage(daysToLast: Int, balance: BigDecimal, actualRecommendedPayment: BigDecimal): ResponseMessageItem =
        when (daysToLast) {
            PaymentSchedulerPeriod.ONE_DAY ->
                responseMessageService.oneDayNotificationMessage(
                    balance = balance,
                    actualRecommendedPayment = actualRecommendedPayment
                )

            PaymentSchedulerPeriod.FIVE_DAYS ->
                responseMessageService.fiveDaysNotificationMessage(
                    balance = balance,
                    actualRecommendedPayment = actualRecommendedPayment
                )

            else -> throw RuntimeException("unknown payment scheduler period '$daysToLast'")
        }
}