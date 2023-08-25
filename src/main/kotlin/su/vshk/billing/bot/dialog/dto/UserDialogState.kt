package su.vshk.billing.bot.dialog.dto

import su.vshk.billing.bot.dao.model.UserEntity

data class UserDialogState(
    val user: UserEntity? = null,
    val state: DialogState? = null
)