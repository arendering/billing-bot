package su.vshk.billing.bot.service.executor

import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import su.vshk.billing.bot.dao.model.Command
import su.vshk.billing.bot.dao.model.UserEntity
import su.vshk.billing.bot.message.ResponseMessageService
import su.vshk.billing.bot.message.dto.ResponseMessageItem
import su.vshk.billing.bot.service.dto.TariffsDto
import su.vshk.billing.bot.util.TariffNormalizer
import su.vshk.billing.bot.util.debugTraceId
import su.vshk.billing.bot.util.getLogger
import su.vshk.billing.bot.web.client.BillingWebClient
import su.vshk.billing.bot.web.dto.manager.GetVgroupsFlt
import su.vshk.billing.bot.web.dto.manager.GetVgroupsRequest

@Service
class TariffsExecutor(
    private val billingWebClient: BillingWebClient,
    private val responseMessageService: ResponseMessageService
): CommandExecutor {

    companion object {
        private val log = getLogger()
    }

    override fun getCommand(): Command =
        Command.TARIFFS

    override fun execute(user: UserEntity, options: Any?): Mono<ResponseMessageItem> =
        Mono.deferContextual { context ->
            log.debugTraceId(context, "try to execute command '${getCommand().value}'")

            val request = GetVgroupsRequest(flt = GetVgroupsFlt(userId = user.uid))
            billingWebClient.getVgroups(request)
                .map { response ->
                    val tariffsDto = response.ret
                        ?.mapNotNull { it.tariffDescription }
                        ?.let { TariffNormalizer.normalizeTariffs(it) }
                        ?: TariffsDto()

                    responseMessageService.tariffsMessage(tariffsDto)
                }
        }
}