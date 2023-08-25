package su.vshk.billing.bot.util

import org.telegram.telegrambots.meta.api.objects.Update

fun Update?.isTextMessage(): Boolean = this?.message?.hasText() == true

fun Update?.isButtonInput(): Boolean = this?.hasCallbackQuery() == true

// бот отправляет сообщение пользователю (например, отправка напоминания об оплате), а пользователь удалил чат с ботом
fun Throwable.botWasBlockedByUser(): Boolean =
    this.message?.lowercase()?.contains("forbidden: bot was blocked by the user") == true

// если новый текст совпадает со старым в редактируемом сообщении (калькулятор суммы в обещанном платеже)
fun Throwable.isMessageNotModified(): Boolean =
    this.message?.lowercase()?.contains("bad request: message is not modified") == true
