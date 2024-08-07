package su.vshk.billing.bot.service

import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import su.vshk.billing.bot.dao.model.EnabledNotificationUserDto
import su.vshk.billing.bot.dao.model.PaymentNotificationMessageEntity
import su.vshk.billing.bot.dao.model.PaymentNotificationType
import su.vshk.billing.bot.dao.service.PaymentNotificationMessageDaoService
import su.vshk.billing.bot.message.dto.RequestMessageItem
import su.vshk.billing.bot.message.dto.ResponseMessageItem
import su.vshk.billing.bot.message.response.NotificationMessageService
import su.vshk.billing.bot.scheduler.PaymentSchedulerPeriod
import su.vshk.billing.bot.service.dto.PaymentNotificationMessageDto
import su.vshk.billing.bot.util.*
import su.vshk.billing.bot.web.dto.manager.GetVgroupsRet

@Service
class PaymentNotificationService(
    private val vgroupsService: VgroupsService,
    private val recommendedPaymentService: RecommendedPaymentService,
    private val paymentNotificationMessageDaoService: PaymentNotificationMessageDaoService,
    private val notificationMessageService: NotificationMessageService
) {

    private val logger = getLogger()

    /**
     * Создает напоминание об оплате.
     */
    fun createPaymentNotification(userDto: EnabledNotificationUserDto, daysToLast: Int): Mono<ResponseMessageItem> =
        Mono
            .defer { doCreatePaymentNotification(userDto = userDto, daysToLast = daysToLast) }
            .onErrorResume {
                Mono.deferContextual { context ->
                    logger.errorTraceId(context, "notification error: ${it.stackTraceToString()}")
                    notificationMessageService.notifyErrorGroup().toMono()
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
        paymentNotificationMessageDaoService.removeByIdSafe(request.chatId)
            .map { notificationMessageService.deleteMessage(request.messageId) }

    /**
     * Удаляет напоминания об оплате (по планировщику).
     */
    fun deletePaymentNotifications(): Mono<List<Pair<Long, ResponseMessageItem>>> =
        paymentNotificationMessageDaoService.removeAllSafe()
            .map { entities ->
                entities.map { e ->
                    Pair(
                        e.telegramId,
                        notificationMessageService.deleteMessage(e.messageId)
                    )
                }
            }

    private fun doCreatePaymentNotification(userDto: EnabledNotificationUserDto, daysToLast: Int): Mono<ResponseMessageItem> =
        Mono.deferContextual { context ->
            vgroupsService.getInternetVgroups(userDto.userId)
                .map { vgroups ->
                    vgroups.filter(agreementId = userDto.agreementId, notificationType = userDto.notificationType)
                }
                .flatMapMany { Flux.fromIterable(it) }
                .flatMap({ vgroup ->
                    recommendedPaymentService.getActual(vgroup.agreementId!!)
                        .map { Pair(vgroup, it) }
                        .putTraceId(context.traceId)
                }, 10) // значение concurrency выбрано произвольно
                .collectList()
                .map { pairs ->
                    pairs
                        .map { (vgroup, actualRecommendedPayment) ->
                            PaymentNotificationMessageDto(
                                address = vgroup.addresses?.firstOrNull()?.address?.let { a -> AddressNormalizer.notificationNormalize(a) }
                                    ?: throw RuntimeException("addresses is empty or null"),
                                balance = vgroup.balance
                                    ?: throw RuntimeException("balance is null"),
                                actualRecommendedPayment = actualRecommendedPayment
                            )
                        }
                        .filterNot { AmountUtils.isZero(it.actualRecommendedPayment) }
                        .let { resolveMessage(daysToLast = daysToLast, paymentMessageDtos = it) }
                }
        }

    private fun List<GetVgroupsRet>.filter(agreementId: Long, notificationType: String) =
        when (notificationType) {
            PaymentNotificationType.SINGLE ->
                this.filter { it.agreementId == agreementId }

            PaymentNotificationType.ALL ->
                this

            else -> throw RuntimeException("unknown notification type: $notificationType")
        }

    private fun resolveMessage(daysToLast: Int, paymentMessageDtos: List<PaymentNotificationMessageDto>): ResponseMessageItem =
        if (paymentMessageDtos.isEmpty()) {
            notificationMessageService.emptyMessage()
        } else {
            when (daysToLast) {
                PaymentSchedulerPeriod.ONE_DAY ->
                    notificationMessageService.oneDayNotification(paymentMessageDtos)

                PaymentSchedulerPeriod.FIVE_DAYS ->
                    notificationMessageService.fiveDaysNotification(paymentMessageDtos)

                else -> throw RuntimeException("unknown payment scheduler period '$daysToLast'")
            }
        }
}