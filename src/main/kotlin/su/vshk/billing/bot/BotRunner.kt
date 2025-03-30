package su.vshk.billing.bot

import su.vshk.billing.bot.util.getLogger
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession

@Component
@ConditionalOnProperty(prefix = "bot", name = ["enabled"], havingValue = "true")
class BotRunner(
    private val bot: Bot
): CommandLineRunner {

    private val logger = getLogger()

    override fun run(vararg args: String?) {
        try {
            TelegramBotsApi(DefaultBotSession::class.java).registerBot(bot)
        } catch (ex: TelegramApiException) {
            logger.error("Unable to register bot", ex)
        }
    }
}