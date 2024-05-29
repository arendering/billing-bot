package su.vshk.billing.bot.message.response

import org.springframework.context.MessageSource
import org.springframework.stereotype.Service
import su.vshk.billing.bot.message.dto.ResponseMessageItem

@Service
class CommonMessageService(
    private val messageSource: MessageSource
): ResponseMessageService(messageSource) {

    fun repeatMenu(messageIdToDelete: Int): ResponseMessageItem =
        ResponseMessageItem(
            content = mainMenuContent(),
            meta = ResponseMessageItem.Meta().sendMessage().deleteMessages(listOf(messageIdToDelete))
        )

    fun repeatLastDialogMessage(messageIdToDelete: Int, content: ResponseMessageItem.Content): ResponseMessageItem =
        ResponseMessageItem(
            content = content,
            meta = ResponseMessageItem.Meta().sendMessage().deleteMessages(listOf(messageIdToDelete))
        )
}