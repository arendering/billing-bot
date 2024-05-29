package su.vshk.billing.bot.message.response

import org.springframework.context.MessageSource
import org.springframework.stereotype.Service
import su.vshk.billing.bot.dao.model.BlockedStatus
import su.vshk.billing.bot.dao.model.Command
import su.vshk.billing.bot.dialog.option.AgreementAvailableOptions
import su.vshk.billing.bot.dialog.option.GenericAvailableOptions
import su.vshk.billing.bot.message.HtmlMarkupFormatter
import su.vshk.billing.bot.message.TextType
import su.vshk.billing.bot.message.dto.ResponseMessageItem
import su.vshk.billing.bot.service.dto.AgreementDto
import su.vshk.billing.bot.service.dto.InfoDto
import su.vshk.billing.bot.util.AmountUtils
import java.math.BigDecimal

@Service
class AgreementMessageService(
    private val messageSource: MessageSource
): ResponseMessageService(messageSource) {

    /**
     * Основная информация по текущему договору.
     */
    fun showInfo(info: InfoDto): ResponseMessageItem =
        createEditMessage(
            content = ResponseMessageItem.Content(
                text = HtmlMarkupFormatter()
                    .addUsername(info.username)
                    .addAgreementNumber(info.agreementNumber)
                    .addMultipleAgreementNoticeIfNeeded(info.multipleAgreements)
                    .addBlocked(info.blocked)
                    .addLineBreak()
                    .addBalance(info.balance)
                    .addDefaultRecommendedPayment(info.defaultRecommendedPayment)
                    .addActualRecommendedPayment(info.actualRecommendedPayment)
                    .addPromiseCreditNoticeIfNeeded(info.promiseCredit)
                    .addLineBreak()
                    .addEmail(info.email)
                    .build(),
                buttons = showInfoButtons(info.multipleAgreements)
            )
        )

    /**
     * Список всех договоров пользователя.
     */
    fun showAgreements(agreements: List<AgreementDto>, actualAgreementId: Long): ResponseMessageItem {
        val actualAgreement = agreements.find { it.agreementId == actualAgreementId }
            ?: throw RuntimeException("actual agreement not found by agreementId '$actualAgreementId'")

        val otherAgreements = agreements.filterNot { it.agreementId == actualAgreementId }

        return createEditMessage(
            content = ResponseMessageItem.Content(
                text = HtmlMarkupFormatter()
                    .addText(text = getText("agreements.switch.main.header"))
                    .addLineBreak()
                    .addText(text = getText("agreements.switch.submain.header"), textType = TextType.BOLD_UNDERLINED)
                    .addActualAgreement(actualAgreement)
                    .addLineBreak()
                    .addOtherAgreements(otherAgreements)
                    .build(),
                buttons = showOtherAgreementsButtons(otherAgreements)
            )
        )
    }

    /**
     * Успешная смена текущего договора.
     */
    fun switchAgreement(chosenAgreement: AgreementDto): ResponseMessageItem =
        createEditMessage(
            content = ResponseMessageItem.Content(
                text = HtmlMarkupFormatter()
                    .addFormattedPair(
                        text = getText("agreements.final.main.template"),
                        first = chosenAgreement.agreementNumber,
                        firstType = TextType.BOLD_ITALIC,
                        second = chosenAgreement.address,
                        secondType = TextType.ITALIC
                    )
                    .addLineBreak()
                    .addText(getText("agreements.final.footer"))
                    .build(),
                buttons = listOf(listOf(getBackToMainMenuKeyboardItem()))
            )
        )

    private fun HtmlMarkupFormatter.addUsername(username: String?): HtmlMarkupFormatter =
        this.addEntry(key = getText("agreements.info.username.key"), value = username)

    private fun HtmlMarkupFormatter.addAgreementNumber(agreementNumber: String?): HtmlMarkupFormatter =
        this.addEntry(key = getText("agreements.info.agreement.number.key"), value = agreementNumber)

    private fun HtmlMarkupFormatter.addMultipleAgreementNoticeIfNeeded(multipleAgreements: Boolean): HtmlMarkupFormatter =
        if (multipleAgreements) {
            this.addText(text = getText("agreements.info.multiple.agreements.notice"), textType = TextType.ITALIC)
        } else {
            this
        }

    private fun HtmlMarkupFormatter.addBlocked(blocked: Long?): HtmlMarkupFormatter =
        blocked
            ?.let { b ->
                this
                    .addEntry(
                        key = getText("agreements.info.blocked.key"),
                        value = resolveBlockedValue(b),
                        valueType = TextType.PLAIN
                    )

                if (blocked != BlockedStatus.ACTIVE) {
                    this.addText(
                        text = resolveBlockedNotice(blocked),
                        textType = TextType.ITALIC
                    )
                }

                this
            }
            ?: this

    private fun resolveBlockedValue(blocked: Long): String =
        if (blocked == BlockedStatus.ACTIVE) {
            getText("agreements.info.blocked.active.emoji")
        } else {
            getText("agreement.info.blocked.not.active.emoji")
        }

    private fun resolveBlockedNotice(blocked: Long): String? =
        when (blocked) {
            in BlockedStatus.BALANCE -> getText("agreement.info.blocked.balance.notice")
            BlockedStatus.USER_BLOCK -> getText("agreement.info.blocked.user.notice")
            BlockedStatus.ADMIN_BLOCK -> getText("agreement.info.blocked.admin.notice")
            BlockedStatus.TRAFFIC_LIMIT -> getText("agreement.info.blocked.traffic.limit.notice")
            BlockedStatus.DISABLED -> getText("agreement.info.blocked.disabled.notice")
            else -> null
        }

    private fun HtmlMarkupFormatter.addBalance(balance: BigDecimal?): HtmlMarkupFormatter =
        this.addEntry(
            key = getText("agreements.info.balance.key"),
            value = balance?.let { AmountUtils.formatAmount(it) }
        )

    private fun HtmlMarkupFormatter.addDefaultRecommendedPayment(defaultRecommendedPayment: BigDecimal?): HtmlMarkupFormatter =
        this.addEntry(
            key = getText("agreements.info.default.recommended.payment.key"),
            value = defaultRecommendedPayment?.let { AmountUtils.formatAmount(it) }
        )

    private fun HtmlMarkupFormatter.addActualRecommendedPayment(actualRecommendedPayment: BigDecimal): HtmlMarkupFormatter =
        if (actualRecommendedPayment > BigDecimal.ZERO) {
            this.addEntry(
                key = getText("agreements.info.actual.recommended.payment.key"),
                value = AmountUtils.formatAmount(actualRecommendedPayment)
            )
        } else {
            this.addText(
                text = getText("agreements.info.actual.recommended.payment.not.required.notice"),
                textType = TextType.ITALIC
            )
        }

    private fun HtmlMarkupFormatter.addPromiseCreditNoticeIfNeeded(promiseCredit: BigDecimal?): HtmlMarkupFormatter =
        this.addEntry(
            key = getText("agreements.info.promise.credit.enabled.notice"),
            value = if (promiseCredit != null && promiseCredit > BigDecimal.ZERO) {
                AmountUtils.formatAmount(promiseCredit)
            } else {
                null
            }
        )

    private fun HtmlMarkupFormatter.addEmail(email: String?): HtmlMarkupFormatter =
        this.addEntry(
            key = getText("agreements.info.email.key"),
            value = if (email.isNullOrEmpty()) getText("agreements.info.email.not.found.notice") else email
        )

    private fun showInfoButtons(multipleAgreements: Boolean): List<List<ResponseMessageItem.InlineKeyboardItem>> =
        if (multipleAgreements) {
            listOf(
                listOf(
                    ResponseMessageItem.InlineKeyboardItem(
                        label = getText("agreements.info.switch.button.label"),
                        callbackData = AgreementAvailableOptions.SWITCH_AGREEMENT
                    )
                ),
                listOf(
                    ResponseMessageItem.InlineKeyboardItem(
                        label = getText("menu.back"),
                        callbackData = AgreementAvailableOptions.CANCEL_INFO
                    )
                )
            )
        } else {
            listOf(listOf(getBackToMainMenuKeyboardItem()))
        }

    private fun HtmlMarkupFormatter.addActualAgreement(actualAgreement: AgreementDto): HtmlMarkupFormatter =
        this.addFormattedPair(
            text = getText("agreements.switch.actual.agreement.template"),
            textType = TextType.ITALIC,
            first = actualAgreement.agreementNumber,
            firstType = TextType.BOLD,
            second = actualAgreement.address,
            secondType = TextType.PLAIN
        )

    private fun HtmlMarkupFormatter.addOtherAgreements(otherAgreements: List<AgreementDto>): HtmlMarkupFormatter {
        otherAgreements
            .withIndex()
            .forEach { (i, agreement) ->
                this.addFormattedPair(
                    text = getText("agreements.switch.other.agreement.template"),
                    textType = TextType.ITALIC,
                    first = agreement.agreementNumber,
                    firstType = TextType.BOLD,
                    second = agreement.address,
                    secondType = TextType.PLAIN
                )
                if (otherAgreements.lastIndex != i) {
                    this.addLineBreak()
                }
            }
        return this
    }

    private fun showOtherAgreementsButtons(otherAgreements: List<AgreementDto>): List<List<ResponseMessageItem.InlineKeyboardItem>> {
        val agreementButtons = otherAgreements.map { a ->
            ResponseMessageItem.InlineKeyboardItem(
                label = getText("agreements.switch.button.label.template").format(a.agreementNumber),
                callbackData = a.agreementId.toString()
            )
        }

        val menuButton = ResponseMessageItem.InlineKeyboardItem(
            label = getText("menu.back"),
            callbackData = AgreementAvailableOptions.CANCEL_SWITCH_AGREEMENT
        )

        return agreementButtons.map { listOf(it) } + listOf(listOf(menuButton))
    }
}