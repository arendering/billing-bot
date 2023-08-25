package su.vshk.billing.bot.service.dto

import su.vshk.billing.bot.web.dto.manager.GetAccountRet
import su.vshk.billing.bot.web.dto.manager.GetVgroupsRet
import java.math.BigDecimal

data class InfoDto(
    val getVgroupsRet: GetVgroupsRet,
    val defaultRecommendedPayment: BigDecimal,
    val actualRecommendedPayment: BigDecimal,
    val getAccountRet: GetAccountRet
)
