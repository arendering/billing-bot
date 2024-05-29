package su.vshk.billing.bot.service.executor

import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import su.vshk.billing.bot.dao.model.Command
import su.vshk.billing.bot.dao.model.UserEntity
import su.vshk.billing.bot.dialog.option.PaymentHistoryOptions
import su.vshk.billing.bot.dialog.option.PaymentHistoryPeriod
import su.vshk.billing.bot.message.dto.ResponseMessageItem
import su.vshk.billing.bot.message.response.PaymentHistoryMessageService
import su.vshk.billing.bot.service.dto.PaymentHistoryDto
import su.vshk.billing.bot.util.AmountUtils
import su.vshk.billing.bot.util.debugTraceId
import su.vshk.billing.bot.util.getLogger
import su.vshk.billing.bot.web.client.BillingWebClient
import su.vshk.billing.bot.web.dto.manager.GetPaymentsFilter
import su.vshk.billing.bot.web.dto.manager.GetPaymentsRequest
import su.vshk.billing.bot.web.dto.manager.GetPaymentsResponse
import su.vshk.billing.bot.web.dto.manager.GetPaymentsRet
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class PaymentHistoryExecutor(
    private val billingWebClient: BillingWebClient,
    private val paymentHistoryMessageService: PaymentHistoryMessageService
): CommandExecutor {

    companion object {
        private val log = getLogger()
        private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    }

    override fun getCommand(): Command =
        Command.PAYMENT_HISTORY

    override fun execute(user: UserEntity, options: Any?): Mono<ResponseMessageItem> =
        Mono.deferContextual { context ->
            options as PaymentHistoryOptions
            log.debugTraceId(context, "try to execute command '${getCommand().value}' with options: ${options}")
            resolveDates(options.period!!)
                .flatMap { (from, to) ->
                    doGetPayments(agreementId = user.agreementId!!, dateFrom = from, dateTo = to)
                        .map { response ->
                            paymentHistoryMessageService.showHistory(
                                toDto(dateFrom = from, dateTo = to, response = response)
                            )
                        }
                }

        }

    private fun resolveDates(period: String): Mono<List<LocalDate>> =
        Mono.fromCallable {
            val today = LocalDate.now()
            val dateFrom = when (period) {
                PaymentHistoryPeriod.ONE_MONTH -> today.minusMonths(1L)
                PaymentHistoryPeriod.THREE_MONTHS -> today.minusMonths(3L)
                PaymentHistoryPeriod.SIX_MONTHS -> today.minusMonths(6L)
                else -> throw IllegalStateException("unreachable code")
            }
            listOf(dateFrom, today)
        }

    private fun doGetPayments(agreementId: Long, dateFrom: LocalDate, dateTo: LocalDate): Mono<GetPaymentsResponse> =
        billingWebClient.getPayments(
            GetPaymentsRequest(
                filter = GetPaymentsFilter(
                    agreementId = agreementId,
                    dateFrom = dateFrom,
                    dateTo = dateTo
                )
            )
        )

    private fun toDto(dateFrom: LocalDate, dateTo: LocalDate, response: GetPaymentsResponse): PaymentHistoryDto =
        response.ret
            ?.sortedBy { it.pay?.dateTime }
            ?.map {
                val pay = it.pay
                val dateTime = pay?.dateTime

                PaymentHistoryDto.PaymentDto(
                    date = dateTime?.format(dateFormatter),
                    time = dateTime?.format(timeFormatter),
                    id = pay?.receipt,
                    amount = pay?.amount,
                    manager = resolveManager(it)
                )
            }
            .let {
                PaymentHistoryDto(
                    dateFrom = dateFrom.format(dateFormatter),
                    dateTo = dateTo.format(dateFormatter),
                    payments = it
                )
            }

    private fun resolveManager(getPaymentRet: GetPaymentsRet): String? =
        getPaymentRet.manager
            ?.ifBlank { null }
            ?.let { mgr ->
                getPaymentRet.managerDescription
                    ?.ifBlank { null }
                    ?.let { "$mgr ($it)" }
                    ?: mgr
            }
}