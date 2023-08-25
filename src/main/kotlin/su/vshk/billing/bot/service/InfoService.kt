package su.vshk.billing.bot.service

import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import su.vshk.billing.bot.web.client.BillingWebClient
import su.vshk.billing.bot.web.dto.manager.GetVgroupsFlt
import su.vshk.billing.bot.web.dto.manager.GetVgroupsRequest
import su.vshk.billing.bot.web.dto.manager.GetVgroupsRet
import java.math.BigDecimal

@Service
class InfoService(
    private val billingWebClient: BillingWebClient
) {
    fun getAgrmId(uid: Long): Mono<Long> =
        getVgroups(uid)
            .map {
                it.agrmId ?: throw RuntimeException("agrmId is null")
            }

    fun getBalance(uid: Long): Mono<BigDecimal> =
        getVgroups(uid)
            .map {
                it.balance ?: throw RuntimeException("balance is null")
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

}