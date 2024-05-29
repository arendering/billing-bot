package su.vshk.billing.bot.message.response

import org.springframework.context.MessageSource
import org.springframework.stereotype.Service
import su.vshk.billing.bot.message.HtmlMarkupFormatter
import su.vshk.billing.bot.message.TextType
import su.vshk.billing.bot.message.dto.ResponseMessageItem

@Service
class ContactsMessageService(
    private val messageSource: MessageSource
): ResponseMessageService(messageSource) {


    /**
     * Показывает контакты.
     */
    fun showContacts(): ResponseMessageItem =
        createEditMessage(
            content = ResponseMessageItem.Content(
                text = HtmlMarkupFormatter()
                    .addSupportInfo()
                    .addLineBreak()
                    .addOfficeInfo()
                    .build(),
                buttons = listOf(listOf(getBackToMainMenuKeyboardItem()))
            )
        )

    private fun HtmlMarkupFormatter.addSupportInfo(): HtmlMarkupFormatter =
        this
            .addText(text = getText("contacts.support.header"), textType = TextType.BOLD_UNDERLINED)
            .addEntry(
                key = getText("contacts.support.operation.key"),
                keyType = TextType.PLAIN,
                value = getText("contacts.support.operation.value"),
            )
            .addEntry(
                key = getText("contacts.support.phone.key"),
                keyType = TextType.PLAIN,
                value = getText("contacts.support.phone.value1"),
            )
            .addEntry(
                key = getText("contacts.support.phone.key"),
                keyType = TextType.PLAIN,
                value = getText("contacts.support.phone.value2"),
            )

    private fun HtmlMarkupFormatter.addOfficeInfo(): HtmlMarkupFormatter =
        this
            .addText(text = getText("contacts.office.header"), textType = TextType.BOLD_UNDERLINED)
            .addEntries(
                key = getText("contacts.office.operation.key"),
                keyType = TextType.PLAIN,
                values = listOf(
                    getText("contacts.office.operation.value1"),
                    getText("contacts.office.operation.value2")
                )
            )
            .addEntry(
                key = getText("contacts.office.operation.lunch.key"),
                keyType = TextType.PLAIN,
                value = getText("contacts.office.operation.lunch.value")
            )
            .addEntry(
                key = getText("contacts.office.address.key"),
                keyType = TextType.PLAIN,
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
}