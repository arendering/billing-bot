package su.vshk.billing.bot.config

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import su.vshk.billing.bot.service.dto.SbssTariffCacheContext
import java.time.Duration

@Configuration
@EnableCaching
class CacheConfig {

    @Bean
    fun sbssTariffsCache(properties: BotProperties): Cache<String, SbssTariffCacheContext> =
        Caffeine
            .newBuilder()
            .expireAfterWrite(Duration.ofHours(properties.cache.sbssKnowledgeExpiredHours))
            .build()
}