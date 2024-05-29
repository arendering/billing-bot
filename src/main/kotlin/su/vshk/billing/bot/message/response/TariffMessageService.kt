package su.vshk.billing.bot.message.response

import org.springframework.context.MessageSource
import org.springframework.stereotype.Service
import su.vshk.billing.bot.message.HtmlMarkupFormatter
import su.vshk.billing.bot.message.TextType
import su.vshk.billing.bot.message.dto.ResponseMessageItem
import su.vshk.billing.bot.service.dto.*

@Service
class TariffMessageService(
    private val messageSource: MessageSource
): ResponseMessageService(messageSource) {

    /**
     * Показывает тарифы пользователя.
     */
    fun showTariffs(dto: TariffDto): ResponseMessageItem =
        createEditMessage(
            content = ResponseMessageItem.Content(
                text = HtmlMarkupFormatter()
                    .addTariffs(header = getText("tariffs.internet.header"), tariffs = dto.internet)
                    .addTariffs(header = getText("tariffs.tv.header"), tariffs = dto.tv)
                    .addTariffs(header = getText("tariffs.combo.header"), tariffs = dto.combo)
                    .addDeprecatedNotice(dto.containsDeprecated)
                    .addText(text = getText("tariffs.footer"), textType = TextType.ITALIC)
                    .build(),
                buttons = listOf(listOf(getBackToMainMenuKeyboardItem()))
            )
        )

    private fun HtmlMarkupFormatter.addTariffs(header: String, tariffs: List<Tariff>): HtmlMarkupFormatter =
        if (tariffs.isEmpty()) {
            this
        } else {
            this
                .addText(text = header, textType = TextType.BOLD)
                .addLineBreak()

            tariffs
                .withIndex()
                .forEach { (i, tariff) ->
                    this
                        .addEntry(
                            key = getText("tariffs.name.key"),
                            keyType = TextType.PLAIN,
                            value = tariff.name,
                            valueType = TextType.ITALIC
                        )
                        .addEntry(
                            key = getText("tariffs.speed.key"),
                            keyType = TextType.PLAIN,
                            value = tariff.speed,
                            valueType = TextType.ITALIC
                        )
                        .addEntry(
                            key = getText("tariffs.channels.key"),
                            keyType = TextType.PLAIN,
                            value = tariff.channels,
                            valueType = TextType.ITALIC
                        )
                        .addEntry(
                            key = getText("tariffs.rent.key"),
                            keyType = TextType.PLAIN,
                            value = tariff.rent,
                            valueType = TextType.ITALIC
                        )

                    if (tariffs.lastIndex != i) {
                        this.addLineBreak()
                    }
                }

            this.addLineBreak()
        }

    private fun HtmlMarkupFormatter.addDeprecatedNotice(containsDeprecated: Boolean): HtmlMarkupFormatter =
        if (containsDeprecated) {
            this
                .addText(text = getText("tariffs.deprecated.notice"), textType = TextType.ITALIC)
                .addLineBreak()
        } else {
            this
        }
}