package su.vshk.billing.bot.service.executor

import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import su.vshk.billing.bot.dao.model.Command
import su.vshk.billing.bot.dao.model.UserEntity
import su.vshk.billing.bot.dialog.option.PromisePaymentOptions
import su.vshk.billing.bot.message.ResponseMessageService
import su.vshk.billing.bot.message.dto.ResponseMessageItem
import su.vshk.billing.bot.util.debugTraceId
import su.vshk.billing.bot.util.getLogger
import su.vshk.billing.bot.web.client.BillingWebClient
import su.vshk.billing.bot.web.dto.client.ClientPromisePaymentRequest

@Service
class PromisePaymentExecutor(
    private val billingWebClient: BillingWebClient,
    private val responseMessageService: ResponseMessageService
): CommandExecutor {

    companion object {
        private val log = getLogger()
    }

    override fun getCommand(): Command =
        Command.PROMISE_PAYMENT

    override fun execute(user: UserEntity, options: Any?): Mono<ResponseMessageItem> =
        Mono.deferContextual { context ->
            options as PromisePaymentOptions
            log.debugTraceId(context, "try to execute command '${getCommand().value}' with options: ${options}")

            val request = ClientPromisePaymentRequest(agrmId = user.agrmId, amount = options.amount)
            billingWebClient.clientPromisePayment(user = user, request = request)
                .map {
                    if (it.isFault()) {
                        val faultString = it.fault?.faultString?.lowercase()
                        when {
                            faultString?.contains("payment is overdue") == true ->
                                responseMessageService.clientPromisePaymentOverdueMessage()

                            faultString?.contains("already assigned") == true ->
                                responseMessageService.clientPromisePaymentAssignedMessage()

                            else ->
                                responseMessageService.clientPromisePaymentErrorMessage()
                        }
                    } else {
                        responseMessageService.clientPromisePaymentSuccessMessage()
                    }
                }
        }
}