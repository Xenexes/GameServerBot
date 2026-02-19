package de.xenexes.gameserverbot.domain.shared

@JvmInline
value class UserId(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "UserId cannot be blank" }
    }

    companion object {
        val SYSTEM = UserId("SYSTEM")
        val CRON_JOB = UserId("CRON_JOB")
    }
}
