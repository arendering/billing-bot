package su.vshk.billing.bot.service.dto

data class TariffDto(
    val internet: List<Tariff> = emptyList(),
    val tv: List<Tariff> = emptyList(),
    val combo: List<Tariff> = emptyList(),
    val containsDeprecated: Boolean = false
)

data class Tariff(
    /**
     * Идентификатор
     */
    val id: Long? = null,
    /**
     * Тип (интернет, ТВ, комбо)
     */
    val type: String? = null,
    /**
     * Название
     */
    val name: String? = null,
    /**
     * Скорость соединения (для интернета)
     */
    val speed: String? = null,
    /**
     * Количество каналов (для ТВ и комбо)
     */
    val channels: String? = null,
    /**
     * Абонентская плата
     */
    val rent: String? = null
)

data class SbssTariffCacheContext(
    val tariffs: Map<Long, Tariff>
)