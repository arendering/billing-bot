package su.vshk.billing.bot.dialog.dto

import su.vshk.billing.bot.dao.model.UserEntity

data class UserStateDto(
    val user: UserEntity,
    val state: StateDto
)
