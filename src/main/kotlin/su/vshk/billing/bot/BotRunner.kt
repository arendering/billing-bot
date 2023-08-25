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

    companion object {
        private val log = getLogger()
    }

    override fun run(vararg args: String?) {
        try {
            val botsApi = TelegramBotsApi(DefaultBotSession::class.java)
            botsApi.registerBot(bot)
        } catch (ex: TelegramApiException) {
            log.error("bot start error", ex)
        }
    }
}