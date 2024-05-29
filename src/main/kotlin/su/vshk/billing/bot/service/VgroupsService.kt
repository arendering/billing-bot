package su.vshk.billing.bot.service

import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import su.vshk.billing.bot.web.client.BillingWebClient
import su.vshk.billing.bot.web.dto.manager.GetVgroupsFilter
import su.vshk.billing.bot.web.dto.manager.GetVgroupsRequest
import su.vshk.billing.bot.web.dto.manager.GetVgroupsRet

@Service
class VgroupsService(
    private val billingWebClient: BillingWebClient
) {
    companion object {
        private val internetDescriptions = listOf("Radius agent", "Netflow")
    }

    /**
     * Получает учетные записи пользователя для услуги "Интернет".
     */
    fun getInternetVgroups(userId: Long): Mono<List<GetVgroupsRet>> =
        getAllVgroups(userId)
            .map { vgroups ->
                vgroups
                    .filter { it.agentDescription in internetDescriptions }
                    .distinctBy { it.agreementId }
                    .ifEmpty { throw RuntimeException("internet vgroups is empty") }
            }

    /**
     * Получает учетные записи пользователя для всех услуг.
     */
    fun getAllVgroups(userId: Long): Mono<List<GetVgroupsRet>> =
        billingWebClient
            .getVgroups(
                GetVgroupsRequest(
                    filter = GetVgroupsFilter(
                        userId = userId
                    )
                )
            )
            .map { response ->
                response.ret
                    ?.ifEmpty { throw RuntimeException("vgroups is empty") }
                    ?: throw RuntimeException("getVgroups payload is null")
            }

}