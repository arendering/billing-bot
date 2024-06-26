package su.vshk.billing.bot.service.processor

import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import su.vshk.billing.bot.dao.model.Command
import su.vshk.billing.bot.dialog.DialogProcessor
import su.vshk.billing.bot.message.dto.RequestMessageItem
import su.vshk.billing.bot.message.dto.ResponseMessageItem
import su.vshk.billing.bot.service.LoginMessageIdService

@Service
class PostProcessorService(
    private val loginMessageIdService: LoginMessageIdService,
    private val dialogProcessor: DialogProcessor
) {

    /**
     * Пост-обработка ответа.
     */
    fun postProcess(request: RequestMessageItem, response: ResponseMessageItem): Mono<Unit> =
        when {
            dialogProcessor.getCommand(request.chatId) == Command.LOGIN ->
                loginMessageIdService.add(telegramId = request.chatId, messageId = response.meta.sendMessage.messageId!!)
                    .then(Mono.empty())

            else -> Mono.empty()
        }
}