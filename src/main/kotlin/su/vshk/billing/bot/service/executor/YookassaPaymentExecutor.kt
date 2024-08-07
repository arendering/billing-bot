package su.vshk.billing.bot.service.executor

import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import su.vshk.billing.bot.config.BotProperties
import su.vshk.billing.bot.dao.model.Command
import su.vshk.billing.bot.dao.model.UserEntity
import su.vshk.billing.bot.dialog.option.YookassaPaymentOptions
import su.vshk.billing.bot.message.dto.ResponseMessageItem
import su.vshk.billing.bot.message.response.YookassaPaymentMessageService
import su.vshk.billing.bot.service.VgroupsService
import su.vshk.billing.bot.util.debugTraceId
import su.vshk.billing.bot.util.getLogger
import su.vshk.billing.bot.web.client.BillingWebClient
import su.vshk.billing.bot.web.client.YookassaWebClient
import su.vshk.billing.bot.web.dto.manager.InsertPrePaymentRequest
import su.vshk.billing.bot.web.dto.manager.InsertPrePaymentRequestValue
import su.vshk.billing.bot.web.dto.yookassa.*

@Service
class YookassaPaymentExecutor(
    private val properties: BotProperties,
    private val yookassaWebClient: YookassaWebClient,
    private val billingWebClient: BillingWebClient,
    private val vgroupsService: VgroupsService,
    private val yookassaPaymentMessageService: YookassaPaymentMessageService
): CommandExecutor {

    private val logger = getLogger()

    override fun getCommand(): Command =
        Command.YOOKASSA_PAYMENT

    override fun execute(user: UserEntity, options: Any?): Mono<ResponseMessageItem> =
        Mono.deferContextual { context ->
            options as YookassaPaymentOptions
            logger.debugTraceId(context, "try to execute command '${getCommand().value}' with options: $options")

            getAgreementNumber(user)
                .flatMap { agreementNumber ->
                    insertPrePayment(user = user, amount = options.amount!!)
                        .flatMap { prePaymentId ->
                            createYookassaPayment(prePaymentId = prePaymentId, agreementNumber = agreementNumber, options = options)
                        }
                }
                .map { yookassaPaymentMessageService.showPaymentLinkMessage(it) }
        }

    private fun getAgreementNumber(user: UserEntity): Mono<String> =
        vgroupsService.getInternetVgroups(user.userId!!)
            .map { vgroups ->
                vgroups.find { it.agreementId == user.agreementId }?.agreementNumber
                    ?: throw RuntimeException("agreementNumber not found for user $user")
            }

    private fun insertPrePayment(user: UserEntity, amount: Int): Mono<Long> =
        billingWebClient
            .insertPrePayment(
                InsertPrePaymentRequest(
                    value = InsertPrePaymentRequestValue(
                        agreementId = user.agreementId!!,
                        amount = amount.toDouble()
                    )
                )
            )
            .map { it.prePaymentId ?: throw RuntimeException("prePaymentId is null") }

    private fun createYookassaPayment(prePaymentId: Long, agreementNumber: String, options: YookassaPaymentOptions): Mono<String> =
        yookassaWebClient
            .createPayment(
                YookassaPayment(
                    amount = YookassaPaymentAmount(
                        value = "${options.amount}.00",
                        currency = "RUB"
                    ),
                    paymentMethodData = YookassaPaymentMethodData(
                        type = "bank_card"
                    ),
                    confirmation = YookassaPaymentConfirmation(
                        type = "redirect",
                        returnUrl = properties.yookassaPayment.returnUrl
                    ),
                    capture = true,
                    description = yookassaPaymentMessageService.getPaymentDescription(agreementNumber),
                    metadata = YookassaPaymentMetadata(
                        prePaymentId = prePaymentId.toString()
                    ),
                    receipt = YookassaPaymentReceipt(
                        items = listOf(
                            YookassaPaymentReceiptItem(
                                description = yookassaPaymentMessageService.getPaymentReceiptDescription(agreementNumber),
                                quantity = "1",
                                amount = YookassaPaymentAmount(
                                    value = "${options.amount}.00",
                                    currency = "RUB"
                                ),
                                vatCode = 1,
                                paymentSubject = "service",
                                paymentMode = "full_payment"
                            )
                        ),
                        taxSystemCode = 3,
                        customer = options.customer
                    )
                )
            )
            .map { it.confirmation?.confirmationUrl ?: throw RuntimeException("confirmation.confirmationUrl is null") }
}