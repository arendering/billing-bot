package su.vshk.billing.bot.message

import su.vshk.billing.bot.service.dto.InfoDto
import su.vshk.billing.bot.service.dto.PaymentsDto
import su.vshk.billing.bot.service.dto.TariffsDto
import org.springframework.stereotype.Service
import su.vshk.billing.bot.message.dto.ResponseMessageItem
import su.vshk.billing.bot.service.dto.LoginMessageDto
import java.math.BigDecimal

@Service
class ResponseMessageService(
    private val contentFormatter: ResponseMessageContentFormatter
) {
    // start
    fun startLoginMessage() =
        ResponseMessageItem(
            content = contentFormatter.startLogin(),
            meta = ResponseMessageItem.Meta().sendMessage()
        )

    fun startPasswordMessage() =
        ResponseMessageItem(
            content = contentFormatter.startPassword(),
            meta = ResponseMessageItem.Meta().sendMessage()
        )

    fun startInvalidCredsMessage(telegramId: Long, loginMessageIds: LoginMessageDto): ResponseMessageItem {
        val meta = ResponseMessageItem.Meta()
            .deleteMessages(loginMessageIds.deleteMessageIds)
            .let { m ->
                loginMessageIds.editMessageId
                    ?.let { id -> m.editMessage(id) }
                    ?: m.sendMessage()
            }

        return ResponseMessageItem(
            content = contentFormatter.startInvalidCreds(),
            meta = meta
        )
    }

    fun startUnexpectedErrorMessage(telegramId: Long, loginMessageIds: LoginMessageDto): ResponseMessageItem {
        val meta = ResponseMessageItem.Meta()
            .deleteMessages(loginMessageIds.deleteMessageIds)
            .notifyErrorGroup()
            .let { m ->
                loginMessageIds.editMessageId
                    ?.let { id -> m.editMessage(id) }
                    ?: m.sendMessage()
            }

        return ResponseMessageItem(
            content = contentFormatter.startUnexpectedError(),
            meta = meta
        )
    }

    fun startSuccessfulRegisterMessage(telegramId: Long, loginMessageIds: LoginMessageDto): ResponseMessageItem {
        val meta = ResponseMessageItem.Meta()
            .deleteMessages(loginMessageIds.deleteMessageIds)
            .let { m ->
                loginMessageIds.editMessageId
                    ?.let { id -> m.editMessage(id) }
                    ?: m.sendMessage()
            }

        return ResponseMessageItem(
            content = contentFormatter.mainMenu(),
            meta = meta
        )
    }

    // menu
    fun mainMenuMessage() =
        ResponseMessageItem(
            content = contentFormatter.mainMenu(),
            meta = ResponseMessageItem.Meta().editMessage()
        )

    fun repeatMenuMessage(messageIdToDelete: Int) =
        ResponseMessageItem(
            content = contentFormatter.mainMenu(),
            meta = ResponseMessageItem.Meta().sendMessage().deleteMessages(listOf(messageIdToDelete))
        )

    // info
    fun infoMessage(dto: InfoDto) =
        createEditMessage(content = contentFormatter.info(dto))

    // payments
    fun paymentsPeriod() =
        createEditMessage(content = contentFormatter.paymentsPeriod())

    fun paymentsWrongPeriod() =
        createEditMessage(content = contentFormatter.paymentsWrongPeriod())

    fun payments(dto: PaymentsDto) =
        createEditMessage(content = contentFormatter.payments(dto))

    // clientPromisePayment
    fun clientPromisePaymentWarningMessage() =
        createEditMessage(content = contentFormatter.clientPromisePaymentWarning())

    fun clientPromisePaymentInvalidWarningMessage() =
        createEditMessage(content = contentFormatter.clientPromisePaymentInvalidWarning())

    fun clientPromisePaymentAmountMessage() =
        createEditMessage(content = contentFormatter.clientPromisePaymentAmount())

    fun clientPromisePaymentDebtsOverdueMessage() =
        createEditMessage(content = contentFormatter.clientPromisePaymentDebtsOverdue())

    fun clientPromisePaymentSuccessMessage() =
        createEditMessage(content = contentFormatter.clientPromisePaymentSuccess())

    fun clientPromisePaymentOverdueMessage() =
        createEditMessage(content = contentFormatter.clientPromisePaymentOverdue())

    fun clientPromisePaymentAssignedMessage() =
        createEditMessage(content = contentFormatter.clientPromisePaymentAssigned())

    fun clientPromisePaymentErrorMessage() =
        createEditMessage(content = contentFormatter.clientPromisePaymentError())

    // notification
    fun oneDayNotificationMessage(balance: BigDecimal, actualRecommendedPayment: BigDecimal) =
        ResponseMessageItem(
            content = contentFormatter.oneDayNotification(balance = balance, actualRecommendedPayment = actualRecommendedPayment),
            meta = ResponseMessageItem.Meta().sendMessage()
        )

    fun fiveDaysNotificationMessage(balance: BigDecimal, actualRecommendedPayment: BigDecimal) =
        ResponseMessageItem(
            content = contentFormatter.fiveDaysNotification(balance = balance, actualRecommendedPayment = actualRecommendedPayment),
            meta = ResponseMessageItem.Meta().sendMessage()
        )

    fun notificationEnableMessage() =
        createEditMessage(content = contentFormatter.notificationEnable())

    fun notificationInvalidEnableMessage() =
        createEditMessage(content = contentFormatter.notificationInvalidEnable())

    fun notificationSuccessEnableMessage() =
        createEditMessage(content = contentFormatter.notificationSuccessEnable())

    fun notificationDisableMessage() =
        createEditMessage(content = contentFormatter.notificationDisable())

    fun notificationInvalidDisableMessage() =
        createEditMessage(content = contentFormatter.notificationInvalidDisable())

    fun notificationSuccessDisableMessage() =
        createEditMessage(content = contentFormatter.notificationSuccessDisable())

    // tariffs
    fun tariffsMessage(dto: TariffsDto) =
        createEditMessage(content = contentFormatter.tariffs(dto))

    // contacts
    fun contactsMessage() =
        createEditMessage(content = contentFormatter.contacts())

    // logout
    fun logoutWarningMessage() =
        createEditMessage(content = contentFormatter.logoutWarning())

    fun logoutInvalidWarningMessage() =
        createEditMessage(content = contentFormatter.logoutInvalidWarning())

    fun logoutSuccessMessage(paymentNotificationMessageId: Int?) =
        ResponseMessageItem(
            content = contentFormatter.logoutSuccess(),
            meta = ResponseMessageItem.Meta().editMessage().deleteMessages(listOfNotNull(paymentNotificationMessageId))
        )

    // other
    fun somethingWentWrongMessage() =
        ResponseMessageItem(
            content = contentFormatter.somethingWentWrong(),
            meta = ResponseMessageItem.Meta().editMessage().notifyErrorGroup()
        )

    fun deleteMessage(messageId: Int) =
        ResponseMessageItem(
            meta = ResponseMessageItem.Meta().deleteMessages(listOf(messageId))
        )

    fun emptyMessage() =
        ResponseMessageItem(
            meta = ResponseMessageItem.Meta()
        )

    fun notifyErrorGroupMessage() =
        ResponseMessageItem(
            meta = ResponseMessageItem.Meta().notifyErrorGroup()
        )

    private fun createEditMessage(content: ResponseMessageItem.Content) =
        ResponseMessageItem(
            content = content,
            meta = ResponseMessageItem.Meta().editMessage()
        )
}