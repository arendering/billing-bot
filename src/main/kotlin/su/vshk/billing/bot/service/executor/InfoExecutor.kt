package su.vshk.billing.bot.service.executor

import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import su.vshk.billing.bot.dao.model.Command
import su.vshk.billing.bot.dao.model.UserEntity
import su.vshk.billing.bot.message.ResponseMessageService
import su.vshk.billing.bot.message.dto.ResponseMessageItem
import su.vshk.billing.bot.service.RecommendedPaymentService
import su.vshk.billing.bot.service.dto.InfoDto
import su.vshk.billing.bot.util.debugTraceId
import su.vshk.billing.bot.util.getLogger
import su.vshk.billing.bot.web.client.BillingWebClient
import su.vshk.billing.bot.web.dto.manager.*

@Service
class InfoExecutor(
    private val billingWebClient: BillingWebClient,
    private val recommendedPaymentService: RecommendedPaymentService,
    private val responseMessageService: ResponseMessageService
): CommandExecutor {

    companion object {
        private val log = getLogger()
    }

    override fun getCommand(): Command =
        Command.INFO

    override fun execute(user: UserEntity, options: Any?): Mono<ResponseMessageItem> =
        Mono.deferContextual { context ->
            log.debugTraceId(context, "try to execute command '${getCommand().value}'")

            getVgroups(user.uid!!)
                .flatMap { getVgroupsRet ->
                    recommendedPaymentService.getDefault(user.agrmId!!)
                        .flatMap { defaultRecommendedPayment ->
                            recommendedPaymentService.getActual(user.agrmId)
                                .flatMap { actualRecommendedPayment ->
                                    getAccountInfo(user.uid)
                                        .map { getAccountRet ->
                                            responseMessageService.infoMessage(
                                                InfoDto(
                                                    getVgroupsRet = getVgroupsRet,
                                                    defaultRecommendedPayment = defaultRecommendedPayment,
                                                    actualRecommendedPayment = actualRecommendedPayment,
                                                    getAccountRet = getAccountRet
                                                )
                                            )
                                        }
                                }
                        }
                }
        }

    private fun getVgroups(uid: Long): Mono<GetVgroupsRet> =
        billingWebClient
            .getVgroups(
                GetVgroupsRequest(
                    flt = GetVgroupsFlt(
                        userId = uid
                    )
                )
            )
            .map {
                it.ret
                    ?.ifEmpty { throw RuntimeException("empty getVgroups response list") }
                    ?.first()
                    ?: throw RuntimeException("getVgroups payload is null")
            }

    private fun getAccountInfo(uid: Long): Mono<GetAccountRet> =
        billingWebClient
            .getAccount(
                GetAccountRequest(uid = uid)
            )
            .map {
                it.ret ?: throw RuntimeException("getAccount payload is null")
            }
}