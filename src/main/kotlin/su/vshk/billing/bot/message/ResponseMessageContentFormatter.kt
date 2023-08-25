package su.vshk.billing.bot.message

import su.vshk.billing.bot.service.dto.PaymentsDto
import su.vshk.billing.bot.service.dto.InfoDto
import su.vshk.billing.bot.service.dto.TariffsDto
import su.vshk.billing.bot.util.AmountUtils
import org.springframework.context.MessageSource
import org.springframework.stereotype.Component
import su.vshk.billing.bot.dao.model.Command
import su.vshk.billing.bot.dao.model.GenericCommand
import su.vshk.billing.bot.dialog.option.*
import su.vshk.billing.bot.message.dto.ResponseMessageItem
import java.math.BigDecimal
import java.util.Locale

@Component
class ResponseMessageContentFormatter(
    private val messageSource: MessageSource
) {
    companion object {
        private const val LINE_SEPARATOR = "\n"
        private const val RUB = '₽'
        private val LOCALE_RU = Locale("ru", "RU")
    }
    
    // start
    fun startLogin() =
        createContent(
            text = getText("start.greeting") + LINE_SEPARATOR + getText("start.login"),
        )

    fun startPassword() =
        createContent(text = getText("start.password"))

    fun startInvalidCreds() =
        createContent(
            text = getText("start.invalid.creds"),
            buttons = registerButton()
        )

    fun startUnexpectedError() =
        createContent(
            text = getText("error.user"),
            buttons = registerButton()
        )

    // menu
    fun mainMenu() =
        HtmlMarkupFormatter()
            .addText(text = getText("menu.main.text"), textType = TextType.BOLD_ITALIC)
            .build()
            .let { text ->
                createContent(
                    text = text,
                    buttons = listOf(
                        listOf(
                            ResponseMessageItem.InlineKeyboardItem(
                                label = getText("menu.info"),
                                callbackData = Command.INFO.value
                            )
                        ),
                        listOf(
                            ResponseMessageItem.InlineKeyboardItem(
                                label = getText("menu.payments"),
                                callbackData = Command.PAYMENTS.value
                            ),
                            ResponseMessageItem.InlineKeyboardItem(
                                label = getText("menu.promise.payment"),
                                callbackData = Command.PROMISE_PAYMENT.value
                            )
                        ),
                        listOf(
                            ResponseMessageItem.InlineKeyboardItem(
                                label = getText("menu.tariffs"),
                                callbackData = Command.TARIFFS.value
                            ),
                            ResponseMessageItem.InlineKeyboardItem(
                                label = getText("menu.notification"),
                                callbackData = Command.NOTIFICATION.value
                            )
                        ),
                        listOf(
                            ResponseMessageItem.InlineKeyboardItem(
                                label = getText("menu.contacts"),
                                callbackData = Command.CONTACTS.value
                            ),
                            ResponseMessageItem.InlineKeyboardItem(
                                label = getText("menu.exit"),
                                callbackData = Command.EXIT.value
                            )
                        )
                    )
                )
            }

    // info
    fun info(dto: InfoDto): ResponseMessageItem.Content {
        val getVgroupsRet = dto.getVgroupsRet
        val defaultRecommendedPayment = dto.defaultRecommendedPayment
        val actualRecommendedPayment = dto.actualRecommendedPayment
        val getAccountRet = dto.getAccountRet

        val promiseCreditValue =
            getAccountRet.agreements
                ?.ifEmpty { null }
                ?.first()
                ?.promiseCredit
                ?.let {
                    if (it > BigDecimal.ZERO) {
                        "${AmountUtils.formatAmount(it)} $RUB"
                    } else {
                        null
                    }
            }

        val balanceValue = getVgroupsRet.balance?.let { "${AmountUtils.formatAmount(it)} $RUB" }
        val defaultRecommendedPaymentValue = "${AmountUtils.formatAmount(defaultRecommendedPayment)} $RUB"

        val htmlMarkupFormatter = HtmlMarkupFormatter()
            .addValue(header = getText("info.username"), value = getVgroupsRet.username)
            .addValue(header = getText("info.agrmnum"), value = getVgroupsRet.agrmNum)
            .addLineBreak()
            .addValue(header = getText("info.balance"), value = balanceValue)
            .addValue(header = getText("info.default.recommended.payment"), value = defaultRecommendedPaymentValue)

        if (actualRecommendedPayment > BigDecimal.ZERO) {
            val actualRecommendedPaymentValue = "${AmountUtils.formatAmount(actualRecommendedPayment)} $RUB"
            htmlMarkupFormatter.addValue(header = getText("info.actual.recommended.payment"), value = actualRecommendedPaymentValue)
        } else {
            htmlMarkupFormatter.addText(text = getText("info.actual.recommended.payment.not.required"), textType = TextType.ITALIC)
        }

        val emailValue = getAccountRet.account?.email.let {
            if (it.isNullOrEmpty()) {
                getText("info.email.not.found")
            } else {
                it
            }
        }
        return htmlMarkupFormatter
            .addValue(header = getText("info.promise.credit"), value = promiseCreditValue)
            .addLineBreak()
            .addValue(header = getText("info.email"), value = emailValue)
            .build()
            .let { createContent(text = it, buttons = backToMainMenuButton()) }
    }


    // payments
    fun paymentsPeriod() =
        createContent(
            text = getText("payments.period"),
            buttons = paymentsPeriodButtons()
        )

    fun paymentsWrongPeriod() =
        createContent(
            text = getText("payments.period.error"),
            buttons = paymentsPeriodButtons()
        )

    fun payments(dto: PaymentsDto): ResponseMessageItem.Content {
        val markupFormatter = HtmlMarkupFormatter()
            .addText(text = getText("payments.response.header", dto.dateFrom, dto.dateTo), textType = TextType.BOLD_UNDERLINED)

        dto.payments
            ?.ifEmpty { null }
            ?.forEach { dto ->
                val amountValue = dto.amount?.let { "$it $RUB" }
                markupFormatter
                    .addLineBreak()
                    .addValue(header = getText("payments.response.date"), headerType = TextType.PLAIN, value = dto.date)
                    .addValue(header = getText("payments.response.time"), headerType = TextType.PLAIN, value = dto.time)
                    .addValue(header = getText("payments.response.id"), headerType = TextType.PLAIN, value = dto.id)
                    .addValue(header = getText("payments.response.amount"), headerType = TextType.PLAIN, value = amountValue)
                    .addValue(header = getText("payments.response.manager"), headerType = TextType.PLAIN, value = dto.manager)
            }
            ?: markupFormatter
                .addLineBreak()
                .addText(text = getText("empty.data"))

        return createContent(text = markupFormatter.build(), buttons = backToMainMenuButton())
    }

    private fun paymentsPeriodButtons() =
        listOf(
            listOf(
                ResponseMessageItem.InlineKeyboardItem(
                    label = getText("payments.period.one.month"),
                    callbackData = PaymentsAvailableOptions.PERIOD_ONE_MONTH
                ),
                ResponseMessageItem.InlineKeyboardItem(
                    label = getText("payments.period.three.months"),
                    callbackData = PaymentsAvailableOptions.PERIOD_THREE_MONTHS
                ),
                ResponseMessageItem.InlineKeyboardItem(
                    label = getText("payments.period.six.months"),
                    callbackData = PaymentsAvailableOptions.PERIOD_SIX_MONTHS
                )
            ),
            listOf(getCancelButton())
        )

    // clientPromisePayment
    fun clientPromisePaymentWarning(): ResponseMessageItem.Content =
        HtmlMarkupFormatter()
            .addValues(
                header = getText("promise.payment.warning.header"),
                values = listOf(
                    getText("promise.payment.warning.body")
                ),
                valueType = TextType.PLAIN
            )
            .addLineBreak()
            .addText(
                text = getText("promise.payment.warning.tail"),
                textType = TextType.ITALIC
            )
            .let {
                createContent(
                    text = it.build(),
                    buttons = clientPromisePaymentWarningButtons()
                )
            }

    fun clientPromisePaymentInvalidWarning() =
        createContent(
            text = getText("promise.payment.warning.invalid"),
            buttons = clientPromisePaymentWarningButtons()
        )

    fun clientPromisePaymentAmountTextTemplate(): String =
        clientPromisePaymentAmountBaseMessageFormatter().build()

    fun clientPromisePaymentAmount() =
        createContent(
            text = "", // сюда будет подставляться значение из шаблона
            buttons = listOf(
                listOf(
                    ResponseMessageItem.InlineKeyboardItem(
                        label = getText("promise.payment.amount.calculator.plus.one") + " $RUB",
                        callbackData = PromisePaymentAvailableOptions.AMOUNT_PLUS_ONE
                    ),
                    ResponseMessageItem.InlineKeyboardItem(
                        label = getText("promise.payment.amount.calculator.minus.one") + " $RUB",
                        callbackData = PromisePaymentAvailableOptions.AMOUNT_MINUS_ONE
                    )
                ),
                listOf(
                    ResponseMessageItem.InlineKeyboardItem(
                        label = getText("promise.payment.amount.calculator.plus.twenty.five") + " $RUB",
                        callbackData = PromisePaymentAvailableOptions.AMOUNT_PLUS_TWENTY_FIVE
                    ),
                    ResponseMessageItem.InlineKeyboardItem(
                        label = getText("promise.payment.amount.calculator.minus.twenty.five") + " $RUB",
                        callbackData = PromisePaymentAvailableOptions.AMOUNT_MINUS_TWENTY_FIVE
                    )
                ),
                listOf(
                    ResponseMessageItem.InlineKeyboardItem(
                        label = getText("promise.payment.amount.calculator.plus.one.hundred") + " $RUB",
                        callbackData = PromisePaymentAvailableOptions.AMOUNT_PLUS_ONE_HUNDRED
                    ),
                    ResponseMessageItem.InlineKeyboardItem(
                        label = getText("promise.payment.amount.calculator.minus.one.hundred") + " $RUB",
                        callbackData = PromisePaymentAvailableOptions.AMOUNT_MINUS_ONE_HUNDRED
                    )
                ),
                listOf(
                    ResponseMessageItem.InlineKeyboardItem(
                        label = getText("promise.payment.amount.calculator.submit"),
                        callbackData = PromisePaymentAvailableOptions.AMOUNT_SUBMIT
                    ),
                ),
                listOf(
                    getCancelButton()
                )
            )
        )

    fun clientPromisePaymentDebtsOverdue() =
        createContent(
            text = getText("promise.payment.debts.overdue"),
            buttons = backToMainMenuButton()
        )

    fun clientPromisePaymentAmountInvalidOptionTextTemplate(): String =
        clientPromisePaymentAmountBaseMessageFormatter()
            .addText(text = getText("promise.payment.amount.footer.invalid") + " $RUB", textType = TextType.ITALIC)
            .build()

    fun clientPromisePaymentSuccess() =
        createContent(
            text = getText("promise.payment.success"),
            buttons = backToMainMenuButton()
        )

    fun clientPromisePaymentOverdue() =
        createContent(
            text = getText("promise.payment.overdue"),
            buttons = backToMainMenuButton()
        )

    fun clientPromisePaymentAssigned() =
        createContent(
            text = getText("promise.payment.assigned"),
            buttons = backToMainMenuButton()
        )

    fun clientPromisePaymentError() =
        createContent(
            text = getText("promise.payment.error"),
            buttons = backToMainMenuButton()
        )

    private fun clientPromisePaymentWarningButtons() =
        listOf(
            listOf(
                ResponseMessageItem.InlineKeyboardItem(
                    label = getText("promise.payment.warning.approve"),
                    callbackData = PromisePaymentAvailableOptions.WARNING_APPROVE
                ),
                getCancelButton()
            )
        )

    private fun clientPromisePaymentAmountBaseMessageFormatter(): HtmlMarkupFormatter =
        HtmlMarkupFormatter()
            .addText(text = getText("promise.payment.amount.header"))
            .addLineBreak()
            .addValue(
                header = getText("promise.payment.amount.body.header"),
                headerType = TextType.BOLD_UNDERLINED,
                value = getText("promise.payment.amount.body.value") + " $RUB",
                valueType = TextType.BOLD
            )

    // notification
    fun fiveDaysNotification(balance: BigDecimal, actualRecommendedPayment: BigDecimal): ResponseMessageItem.Content =
        HtmlMarkupFormatter()
            .addText(text = getText("notification.main.greeting.bell.emoji"))
            .addText(text = getText("notification.main.greeting.header"))
            .addFormattedText(
                text = getText("notification.main.body"),
                textType = TextType.PLAIN,
                value = getText("notification.main.five.days"),
                valueType = TextType.PLAIN
            )
            .addValue(
                header = getText("notification.main.balance"),
                headerType = TextType.PLAIN,
                value = AmountUtils.formatAmount(balance) + " $RUB",
                valueType = TextType.ITALIC
            )
            .addFormattedText(
                text = getText("notification.main.actual.recommended.payment"),
                textType = TextType.PLAIN,
                value = AmountUtils.formatAmount(actualRecommendedPayment) + " $RUB",
                valueType = TextType.ITALIC
            )
            .let { createContent(text = it.build(), buttons = understandButton()) }

    fun oneDayNotification(balance: BigDecimal, actualRecommendedPayment: BigDecimal) =
        HtmlMarkupFormatter()
            .addText(text = getText("notification.main.greeting.bell.and.warning.emoji"))
            .addText(text = getText("notification.main.greeting.header"))
            .addFormattedText(
                text = getText("notification.main.body"),
                textType = TextType.PLAIN,
                value = getText("notification.main.one.day"),
                valueType = TextType.BOLD
            )
            .addValue(
                header = getText("notification.main.balance"),
                headerType = TextType.PLAIN,
                value = AmountUtils.formatAmount(balance) + " $RUB",
                valueType = TextType.ITALIC
            )
            .addFormattedText(
                text = getText("notification.main.actual.recommended.payment"),
                textType = TextType.BOLD,
                value = AmountUtils.formatAmount(actualRecommendedPayment) + " $RUB",
                valueType = TextType.ITALIC
            )
            .let { createContent(text = it.build(), buttons = understandButton()) }

    fun notificationEnable() =
        HtmlMarkupFormatter()
            .addFormattedText(
                text = getText("notification.switch.header"),
                textType = TextType.PLAIN,
                value = getText("notification.switch.header.disabled"),
                valueType = TextType.PLAIN_UNDERLINED
            )
            .addFormattedText(
                text = getText("notification.switch.tail"),
                textType = TextType.PLAIN,
                value = getText("notification.switch.tail.enable"),
                valueType = TextType.PLAIN
            )
            .let { createContent(text = it.build(), buttons = getEnableNotificationButtons()) }

    fun notificationInvalidEnable() =
        createContent(
            text = getText("notification.enable.error"),
            buttons = getEnableNotificationButtons()
        )

    fun notificationSuccessEnable() =
        HtmlMarkupFormatter()
            .addText(text = getText("notification.enable.success"), textType = TextType.BOLD)
            .addText(text = getText("notification.enable.success.tail"), textType = TextType.PLAIN)
            .let { createContent(text = it.build(), buttons = backToMainMenuButton()) }

    fun notificationDisable() =
        HtmlMarkupFormatter()
            .addFormattedText(
                text = getText("notification.switch.header"),
                textType = TextType.PLAIN,
                value = getText("notification.switch.header.enabled"),
                valueType = TextType.PLAIN_UNDERLINED
            )
            .addFormattedText(
                text = getText("notification.switch.tail"),
                textType = TextType.PLAIN,
                value = getText("notification.switch.tail.disable"),
                valueType = TextType.PLAIN
            )
            .let { createContent(text = it.build(), buttons = getDisableNotificationButtons()) }


    fun notificationInvalidDisable() =
        createContent(
            text = getText("notification.disable.error"),
            buttons = getDisableNotificationButtons()
        )

    fun notificationSuccessDisable() =
        createContent(
            text = getText("notification.disable.success"),
            buttons = backToMainMenuButton()
        )

    private fun understandButton() =
        listOf(
            listOf(
                ResponseMessageItem.InlineKeyboardItem(
                    label = getText("notification.understand"),
                    callbackData = GenericCommand.DELETE_PAYMENT_NOTIFICATION
                )
            )
        )

    private fun getEnableNotificationButtons() =
        listOf(
            listOf(
                ResponseMessageItem.InlineKeyboardItem(
                    label = getText("notification.enable.label"),
                    callbackData = NotificationAvailableOptions.TURN_ON
                ),
                getCancelButton()
            )
        )

    private fun getDisableNotificationButtons() =
        listOf(
            listOf(
                ResponseMessageItem.InlineKeyboardItem(
                    label = getText("notification.disable.label"),
                    callbackData = NotificationAvailableOptions.TURN_OFF
                ),
                getCancelButton()
            )
        )

    // tariffs
    fun tariffs(dto: TariffsDto): ResponseMessageItem.Content {
        val internetTariffs = dto.internet
        val tvTariffs = dto.onlineTv
        val htmlMarkupFormatter = HtmlMarkupFormatter()

        if (internetTariffs.isNullOrEmpty() && tvTariffs.isNullOrEmpty()) {
            htmlMarkupFormatter
                .addText(text = getText("empty.data"), textType = TextType.ITALIC)
        } else {
            htmlMarkupFormatter
                .addValues(header = getText("tariffs.internet.header"), values = internetTariffs)
                .addLineBreak()
                .addValues(header = getText("tariffs.tv.header"), values = tvTariffs)
        }

        return createContent(text = htmlMarkupFormatter.build(), buttons = backToMainMenuButton())
    }

    // contacts
    fun contacts(): ResponseMessageItem.Content =
        HtmlMarkupFormatter()
            .addText(text = getText("contacts.support.header"), textType = TextType.BOLD_UNDERLINED)
            .addValue(
                header = getText("contacts.support.operation.header"),
                headerType = TextType.PLAIN,
                value = getText("contacts.support.operation.value"),
            )
            .addValue(
                header = getText("contacts.support.phone.header"),
                headerType = TextType.PLAIN,
                value = getText("contacts.support.phone.number1"),
            )
            .addValue(
                header = getText("contacts.support.phone.header"),
                headerType = TextType.PLAIN,
                value = getText("contacts.support.phone.number2"),
            )
            .addLineBreak()
            .addText(text = getText("contacts.office.header"), textType = TextType.BOLD_UNDERLINED)
            .addValues(
                header = getText("contacts.office.operation.header"),
                headerType = TextType.PLAIN,
                values = listOf(
                    getText("contacts.office.operation.weekdays"),
                    getText("contacts.office.operation.weekend")
                )
            )
            .addValue(
                header = getText("contacts.office.operation.lunch.header"),
                headerType = TextType.PLAIN,
                value = getText("contacts.office.operation.lunch.value")
            )
            .addValue(
                header = getText("contacts.office.address.header"),
                headerType = TextType.PLAIN,
                value = getText("contacts.office.address.value")
            )
            .addLineBreak()
            .addHref(
                text = getText("contacts.office.profile.label"),
                href = getText("contacts.office.profile.link")
            )
            .addLineBreak()
            .addHref(
                text = getText("contacts.office.map.label"),
                href = getText("contacts.office.map.link")
            )
            .let { createContent(text = it.build(), buttons = backToMainMenuButton()) }

    // logout
    fun logoutWarning() =
        createContent(
            text = getText("logout.warning"),
            buttons = getLogoutWarningButtons()
        )

    fun logoutInvalidWarning() =
        createContent(
            text = getText("logout.warning.invalid"),
            buttons = getLogoutWarningButtons()
        )

    fun logoutSuccess() =
        createContent(
            text = getText("logout.success"),
            buttons = registerButton()
        )

    private fun getLogoutWarningButtons() =
        listOf(
            listOf(
                ResponseMessageItem.InlineKeyboardItem(
                    label = getText("logout.warning.button.yes"),
                    callbackData = ExitAvailableOptions.YES
                ),
                getCancelButton()
            )
        )

    // other
    fun somethingWentWrong() =
        createContent(
            text = getText("error.user"),
            buttons = backToMainMenuButton()
        )

    fun errorGroupSomethingWentWrong(traceId: String) =
        createContent(
            text = getText("error.group") + LINE_SEPARATOR + traceId
        )

    private fun getText(code: String, vararg args: String): String =
        messageSource.getMessage(code, args, LOCALE_RU)

    private fun createContent(
        text: String,
        buttons: List<List<ResponseMessageItem.InlineKeyboardItem>> = emptyList()
    ) = ResponseMessageItem.Content(text = text, buttons = buttons)

    private fun getCancelButton() =
        ResponseMessageItem.InlineKeyboardItem(
            label = getText("cancel"),
            callbackData = GenericAvailableOptions.CANCEL
        )

    private fun backToMainMenuButton() =
        listOf(
            listOf(
                ResponseMessageItem.InlineKeyboardItem(
                    label = getText("menu.back"),
                    callbackData = Command.MENU.value
                )
            )
        )

    private fun registerButton() =
        listOf(
            listOf(
                ResponseMessageItem.InlineKeyboardItem(
                    label = getText("start.register"),
                    callbackData = Command.START.value
                )
            )
        )
}