package su.vshk.billing.bot.service

import com.github.benmanes.caffeine.cache.Cache
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import su.vshk.billing.bot.service.dto.SbssTariffCacheContext
import su.vshk.billing.bot.service.dto.Tariff
import su.vshk.billing.bot.util.SbssTariffParser
import su.vshk.billing.bot.util.getLogger
import su.vshk.billing.bot.util.warnTraceId
import su.vshk.billing.bot.web.client.BillingWebClient
import su.vshk.billing.bot.web.dto.manager.GetSbssKnowledgeRequest

@Service
class CachedSbssKnowledgeService(
    private val billingWebClient: BillingWebClient,
    private val cache: Cache<String, SbssTariffCacheContext>
) {

    companion object {
        private val logger = getLogger()
        private const val CACHE_KEY = "sbss-knowledge-key"
    }

    /**
     * Получает тарифы из базы знаний биллинга.
     */
    fun getTariffDatabase(): Mono<Map<Long, Tariff>> =
        Mono.defer {
            cache.getIfPresent(CACHE_KEY)
                ?.let { it.tariffs.toMono() }
                ?: getSbssKnowledge()
                    .map { tariffs ->
                        if (tariffs.isNotEmpty()) {
                            cache.put(CACHE_KEY, SbssTariffCacheContext(tariffs))
                        }
                        tariffs
                    }
        }

    private fun getSbssKnowledge(): Mono<Map<Long, Tariff>> =
        Mono.deferContextual { context ->
            billingWebClient
                .getSbssKnowledge(
                    GetSbssKnowledgeRequest(id = 3)
                )
                .map { response ->
                    val rawTariffs = response.ret?.posts?.mapNotNull { it.post?.text }

                    if (rawTariffs.isNullOrEmpty()) {
                        logger.warnTraceId(context, "raw tariffs from sbss knowledge is null or empty")
                        emptyMap()
                    } else {
                        rawTariffs
                            .mapNotNull { SbssTariffParser.parse(it) }
                            .associateBy { it.id!! }
                    }
                }
        }
}