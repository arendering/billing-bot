package su.vshk.billing.bot.web.dto

class RetryConfig(
    var attempts: Long = 3,
    var waitTimeMillis: Long = 1000
)