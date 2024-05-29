package su.vshk.billing.bot.service.dto

import java.math.BigDecimal

data class InfoDto(
    /**
     * ФИО
     */
    val username: String?,
    /**
     * Номер договора
     */
    val agreementNumber: String?,
    /**
     * Статус интернета
     */
    val blocked: Long? = null,
    /**
     * Баланс
     */
    val balance: BigDecimal?,
    /**
     * Несколько ли договоров у пользователя
     */
    val multipleAgreements: Boolean,
    /**
     * Сумма ежемесячного платежа
     */
    val defaultRecommendedPayment: BigDecimal,
    /**
     * Рекомендуемая к внесению сумма
     */
    val actualRecommendedPayment: BigDecimal,
    /**
     * Сумма обещанного платежа
     */
    val promiseCredit: BigDecimal?,
    /**
     * Адрес электронной почты
     */
    val email: String?
)
