package de.xenexes.gameserverbot.domain.gameserver

enum class GameServerStatus {
    STARTED,
    STOPPED,
    SUSPENDED,
    STOPPING,
    RESTARTING,
    GUARDIAN_LOCKED,
    BACKUP_RESTORE,
    BACKUP_CREATION,
    CHUNKFIX,
    OVERVIEWMAP_RENDER,
    GS_INSTALLATION,
    UNKNOWN,
    ;

    fun canStart(): Boolean = this in setOf(STOPPED, SUSPENDED)

    fun canStop(): Boolean = this == STARTED

    fun canRestart(): Boolean = this == STARTED

    fun isHealthy(): Boolean = this == STARTED

    fun isInMaintenance(): Boolean =
        this in
            setOf(
                BACKUP_RESTORE,
                BACKUP_CREATION,
                CHUNKFIX,
                OVERVIEWMAP_RENDER,
                GS_INSTALLATION,
            )
}
