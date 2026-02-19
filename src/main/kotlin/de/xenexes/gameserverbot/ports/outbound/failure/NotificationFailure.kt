package de.xenexes.gameserverbot.ports.outbound.failure

sealed interface NotificationFailure {
    data class SendFailed(
        val reason: String,
    ) : NotificationFailure
}
