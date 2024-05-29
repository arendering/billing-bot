package su.vshk.billing.bot.message.response

import org.springframework.context.MessageSource
import org.springframework.stereotype.Service
import su.vshk.billing.bot.dao.model.GenericCommand
import su.vshk.billing.bot.dialog.option.NotificationAvailableOptions
import su.vshk.billing.bot.message.HtmlMarkupFormatter
import su.vshk.billing.bot.message.TextType
import su.vshk.billing.bot.message.dto.ResponseMessageItem
import su.vshk.billing.bot.service.dto.PaymentNotificationMessageDto
import su.vshk.billing.bot.util.AmountUtils

@Service
class NotificationMessageService(
    private val messageSource: MessageSource
): ResponseMessageService(messageSource) {

    /**
     * Отключить оповещение для пользователя с одним договором.
     */
    fun disable(): ResponseMessageItem =
        createEditMessage(
            content = ResponseMessageItem.Content(
                text = HtmlMarkupFormatter()
                    .addText(text = getText("notification.disable.for.single.agreement"))
                    .build(),
                buttons = listOf(
                    listOf(
                        ResponseMessageItem.InlineKeyboardItem(
                            label = getText("notification.disable.button.label"),
                            callbackData = NotificationAvailableOptions.DISABLE
                        )
                    ),
                    listOf(getCancelKeyboardItem())
                )
            )
        )

    /**
     * Включить оповещение, содержащее информацию по всем договором или отключить оповещение.
     */
    fun enableForAllAgreementsOrDisable(): ResponseMessageItem =
        createEditMessage(
            content = ResponseMessageItem.Content(
                text = HtmlMarkupFormatter()
                    .addText(text = getText("notification.enable.for.all.agreements.or.disable"))
                    .build(),
                buttons = listOf(
                    listOf(
                        ResponseMessageItem.InlineKeyboardItem(
                            label = getText("notification.enable.for.all.agreements.button.label"),
                            callbackData = NotificationAvailableOptions.ENABLE_FOR_ALL_AGREEMENTS
                        )
                    ),
                    listOf(
                        ResponseMessageItem.InlineKeyboardItem(
                            label = getText("notification.disable.button.label"),
                            callbackData = NotificationAvailableOptions.DISABLE
                        )
                    ),
                    listOf(
                        getCancelKeyboardItem()
                    )
                )
            )
        )

    /**
     * Включить оповещение, содержащее информацию по текущему договору или отключить оповещение.
     */
    fun enableForSingleAgreementOrDisable(): ResponseMessageItem =
        createEditMessage(
            content = ResponseMessageItem.Content(
                text = HtmlMarkupFormatter()
                    .addText(text = getText("notification.enable.for.single.agreement.or.disable"))
                    .build(),
                buttons = listOf(
                    listOf(
                        ResponseMessageItem.InlineKeyboardItem(
                            label = getText("notification.enable.for.single.agreement.button.label"),
                            callbackData = NotificationAvailableOptions.ENABLE_FOR_SINGLE_AGREEMENT
                        )
                    ),
                    listOf(
                        ResponseMessageItem.InlineKeyboardItem(
                            label = getText("notification.disable.button.label"),
                            callbackData = NotificationAvailableOptions.DISABLE
                        )
                    ),
                    listOf(
                        getCancelKeyboardItem()
                    )
                )
            )
        )

    /**
     * Включить оповещение для пользователя с одним договором.
     */
    fun enable(): ResponseMessageItem =
        createEditMessage(
            content = ResponseMessageItem.Content(
                text = HtmlMarkupFormatter()
                    .addText(text = getText("notification.enable.for.single.agreement"))
                    .build(),
                buttons = listOf(
                    listOf(
                        ResponseMessageItem.InlineKeyboardItem(
                            label = getText("notification.enable.button.label"),
                            callbackData = NotificationAvailableOptions.ENABLE
                        )
                    ),
                    listOf(
                        getCancelKeyboardItem()
                    )
                )
            )
        )

    /**
     * Включить оповещение, содержащее информацию по текущему договору или по всем договорам.
     */
    fun enableForSingleAgreementOrEnableForAllAgreements(): ResponseMessageItem =
        createEditMessage(
            content = ResponseMessageItem.Content(
                text = HtmlMarkupFormatter()
                    .addText(text = getText("notification.enable.for.single.agreement.or.enable.for.all.agreements.header"))
                    .addLineBreak()
                    .addText(text = getText("notification.enable.for.single.agreement.or.enable.for.all.agreements.key"), textType = TextType.PLAIN_UNDERLINED)
                    .addText(text = getText("notification.enable.for.single.agreement.or.enable.for.all.agreements.single.value"))
                    .addText(text = getText("notification.enable.for.single.agreement.or.enable.for.all.agreements.single.notice"), textType = TextType.ITALIC)
                    .addLineBreak()
                    .addText(text = getText("notification.enable.for.single.agreement.or.enable.for.all.agreements.all.value"))
                    .addText(text = getText("notification.enable.for.single.agreement.or.enable.for.all.agreements.all.notice"), textType = TextType.ITALIC)
                    .build(),
                buttons = listOf(
                    listOf(
                        ResponseMessageItem.InlineKeyboardItem(
                            label = getText("notification.enable.for.single.agreement.button.label"),
                            callbackData = NotificationAvailableOptions.ENABLE_FOR_SINGLE_AGREEMENT
                        )
                    ),
                    listOf(
                        ResponseMessageItem.InlineKeyboardItem(
                            label = getText("notification.enable.for.all.agreements.button.label"),
                            callbackData = NotificationAvailableOptions.ENABLE_FOR_ALL_AGREEMENTS
                        )
                    ),
                    listOf(
                        getCancelKeyboardItem()
                    )
                )
            )
        )

    /**
     * Оповещение для текущего договора было включено
     */
    fun singleAgreementEnabled(): ResponseMessageItem =
        createEditMessage(
            content = ResponseMessageItem.Content(
                text = HtmlMarkupFormatter()
                    .addText(text = getText("notification.enable.for.single.agreement.success"))
                    .build(),
                buttons = listOf(listOf(getBackToMainMenuKeyboardItem()))
            )
        )

    /**
     * Оповещение для всех договоров было включено
     */
    fun allAgreementsEnabled(): ResponseMessageItem =
        createEditMessage(
            content = ResponseMessageItem.Content(
                text = HtmlMarkupFormatter()
                    .addText(text = getText("notification.enable.for.all.agreements.success"))
                    .build(),
                buttons = listOf(listOf(getBackToMainMenuKeyboardItem()))
            )
        )

    /**
     * Оповещение было включено (для пользователей с одним договором)
     */
    fun enabled(): ResponseMessageItem =
        createEditMessage(
            content = ResponseMessageItem.Content(
                text = HtmlMarkupFormatter()
                    .addText(text = getText("notification.enable.success"))
                    .build(),
                buttons = listOf(listOf(getBackToMainMenuKeyboardItem()))
            )
        )

    /**
     * Оповещение было отключено
     */
    fun disabled(): ResponseMessageItem =
        createEditMessage(
            content = ResponseMessageItem.Content(
                text = HtmlMarkupFormatter()
                    .addText(text = getText("notification.disable.success"))
                    .build(),
                buttons = listOf(listOf(getBackToMainMenuKeyboardItem()))
            )
        )

    /**
     * Оповещение за 1 день до конца месяца
     */
    fun oneDayNotification(dtos: List<PaymentNotificationMessageDto>): ResponseMessageItem =
        ResponseMessageItem(
            content = ResponseMessageItem.Content(
                text = HtmlMarkupFormatter()
                    .addText(text = getText("notification.main.bell.and.warning.emoji"))
                    .addText(text = getText("notification.main.hello.header"))
                    .addFormattedText(
                        text = getText("notification.main.body.template"),
                        textType = TextType.PLAIN,
                        value = getText("notification.main.last.one.day"),
                        valueType = TextType.BOLD
                    )
                    .addAgreementNotification(dtos)
                    .build(),
                buttons = understandButton()
            ),
            meta = ResponseMessageItem.Meta().sendMessage()
        )

    /**
     * Оповещение за 5 дней до конца месяца
     */
    fun fiveDaysNotification(dtos: List<PaymentNotificationMessageDto>): ResponseMessageItem =
        ResponseMessageItem(
            content = ResponseMessageItem.Content(
                text = HtmlMarkupFormatter()
                    .addText(text = getText("notification.main.bell.emoji"))
                    .addText(text = getText("notification.main.hello.header"))
                    .addFormattedText(
                        text = getText("notification.main.body.template"),
                        textType = TextType.PLAIN,
                        value = getText("notification.main.last.five.days"),
                        valueType = TextType.PLAIN
                    )
                    .addAgreementNotification(dtos)
                    .build()
                ,
                buttons = understandButton()
            ),
            meta = ResponseMessageItem.Meta().sendMessage()
        )

    /**
     * Пустое сообщение (если не нужно отправлять оповещение)
     */
    fun emptyMessage(): ResponseMessageItem =
        ResponseMessageItem(
            meta = ResponseMessageItem.Meta()
        )

    /**
     * Отправляет сообщение в группу с ошибками
     */
    fun notifyErrorGroup() =
        ResponseMessageItem(
            meta = ResponseMessageItem.Meta().notifyErrorGroup()
        )

    private fun HtmlMarkupFormatter.addAgreementNotification(dtos: List<PaymentNotificationMessageDto>): HtmlMarkupFormatter {
        this
            .addLineBreak()
            .addText(text = getText("notification.main.address.header"), textType = TextType.PLAIN_UNDERLINED)

        dtos
            .withIndex()
            .forEach { (i, dto) ->
                this.addFormattedText(
                    textType = TextType.PLAIN,
                    text = getText("notification.main.text.template"),
                    valueType = TextType.BOLD,
                    values = arrayOf(
                        dto.address,
                        AmountUtils.formatAmount(amount = dto.balance, splitByThousands = false),
                        AmountUtils.formatAmount(amount = dto.actualRecommendedPayment, splitByThousands = false)
                    )
                )
                if (dtos.lastIndex != i) {
                    this.addLineBreak()
                }
            }

        return this
    }

    private fun understandButton() =
        listOf(
            listOf(
                ResponseMessageItem.InlineKeyboardItem(
                    label = getText("notification.understand"),
                    callbackData = GenericCommand.DELETE_PAYMENT_NOTIFICATION
                )
            )
        )

}