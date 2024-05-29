package su.vshk.billing.bot.service.dto

data class TariffDto(
    val internet: List<Tariff> = emptyList(),
    val tv: List<Tariff> = emptyList(),
    val combo: List<Tariff> = emptyList(),
    val containsDeprecated: Boolean = false
)

data class Tariff(
    val id: Long? = null,
    val type: String? = null,
    val name: String? = null,
    val speed: String? = null,
    val channels: String? = null,
    val rent: String? = null
)

data class SbssTariffCacheContext(
    val tariffs: Map<Long, Tariff>
)