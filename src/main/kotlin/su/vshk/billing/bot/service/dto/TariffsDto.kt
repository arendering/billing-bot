package su.vshk.billing.bot.service.dto

data class TariffsDto(
    val internet: List<String>? = null,
    val onlineTv: List<String>? = null
)
