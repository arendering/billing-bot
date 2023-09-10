package su.vshk.billing.bot.dialog.option

class NotificationAvailableOptions private constructor() {
    companion object {
        const val TURN_ON = "/turn_on"
        const val TURN_OFF = "/turn_off"
    }
}

data class NotificationOptions(
    val enable: Boolean? = null
)
