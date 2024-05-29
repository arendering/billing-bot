package su.vshk.billing.bot.service

import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import su.vshk.billing.bot.web.client.BillingWebClient
import su.vshk.billing.bot.web.dto.manager.GetAccountRequest
import su.vshk.billing.bot.web.dto.manager.GetAccountRet

@Service
class AccountService(
    private val billingWebClient: BillingWebClient
) {

    fun getAccount(userId: Long): Mono<GetAccountRet> =
        billingWebClient
            .getAccount(
                GetAccountRequest(userId = userId)
            )
            .map {
                it.ret ?: throw RuntimeException("getAccount payload is null")
            }
}