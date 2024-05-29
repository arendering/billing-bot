package su.vshk.billing.bot.service.executor

import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import su.vshk.billing.bot.dao.model.Command
import su.vshk.billing.bot.dao.model.TariffType
import su.vshk.billing.bot.dao.model.UserEntity
import su.vshk.billing.bot.message.dto.ResponseMessageItem
import su.vshk.billing.bot.message.response.TariffMessageService
import su.vshk.billing.bot.service.CachedSbssKnowledgeService
import su.vshk.billing.bot.service.VgroupsService
import su.vshk.billing.bot.service.dto.*
import su.vshk.billing.bot.util.DeprecatedTariffNormalizer
import su.vshk.billing.bot.util.debugTraceId
import su.vshk.billing.bot.util.getLogger
import su.vshk.billing.bot.web.dto.manager.GetVgroupsRet

@Service
class TariffsExecutor(
    private val vgroupsService: VgroupsService,
    private val tariffMessageService: TariffMessageService,
    private val cachedSbssKnowledgeService: CachedSbssKnowledgeService
): CommandExecutor {

    private val logger = getLogger()

    override fun getCommand(): Command =
        Command.TARIFFS

    override fun execute(user: UserEntity, options: Any?): Mono<ResponseMessageItem> =
        Mono.deferContextual { context ->
            logger.debugTraceId(context, "try to execute command '${getCommand().value}'")

            getTariffVgroups(user)
                .flatMap { vgroups ->
                    cachedSbssKnowledgeService.getTariffDatabase()
                        .map { sbssDatabase ->
                            val tariffs = resolveTariffs(vgroups = vgroups, sbssDatabase = sbssDatabase)
                            tariffMessageService.showTariffs(tariffs)
                        }
                }
        }

    private fun getTariffVgroups(user: UserEntity): Mono<List<GetVgroupsRet>> =
        vgroupsService.getAllVgroups(user.userId!!)
            .map { vgroups ->
                vgroups
                    .filter { it.agreementId == user.agreementId && it.tariffId != null }
                    .distinctBy { it.tariffId }
                    .ifEmpty { throw RuntimeException("tariff vgroups is empty for agreementId '${user.agreementId}'") }
            }

    private fun resolveTariffs(vgroups: List<GetVgroupsRet>, sbssDatabase: Map<Long, Tariff>): TariffDto =
        vgroups
            .mapNotNull { vgroup ->
                sbssDatabase[vgroup.tariffId]
                    ?: vgroup.tariffDescription?.let { DeprecatedTariffNormalizer.normalize(it) }
            }
            .let { tariffs ->
                TariffDto(
                    internet = tariffs.filter { it.type in TariffType.INTERNET_ALL_TYPES },
                    tv = tariffs.filter { it.type in TariffType.TV_ALL_TYPES },
                    combo = tariffs.filter { it.type == TariffType.COMBO },
                    containsDeprecated = tariffs.any { it.type in TariffType.DEPRECATED_TYPES }
                )
            }
}