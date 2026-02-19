package de.xenexes.gameserverbot.config

import de.xenexes.gameserverbot.domain.discord.DiscordPermissionService
import de.xenexes.gameserverbot.infrastructure.discord.KordDiscordClientGateway
import de.xenexes.gameserverbot.infrastructure.discord.KordSlashCommandRegistration
import de.xenexes.gameserverbot.infrastructure.discord.NoOpDiscordClientGateway
import de.xenexes.gameserverbot.ports.outbound.DiscordClientGateway
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(DiscordProperties::class)
class DiscordConfiguration {
    @Bean
    fun discordPermissionService(): DiscordPermissionService = DiscordPermissionService()

    @Bean
    @ConditionalOnProperty(
        name = ["gameserverbot.discord.enabled"],
        havingValue = "true",
        matchIfMissing = false,
    )
    fun kordSlashCommandRegistration(): KordSlashCommandRegistration = KordSlashCommandRegistration()

    @Bean
    @ConditionalOnProperty(
        name = ["gameserverbot.discord.enabled"],
        havingValue = "true",
        matchIfMissing = false,
    )
    fun kordDiscordClientGateway(
        properties: DiscordProperties,
        slashCommandRegistration: KordSlashCommandRegistration,
    ): KordDiscordClientGateway = KordDiscordClientGateway(properties, slashCommandRegistration)

    @Bean
    @ConditionalOnProperty(
        name = ["gameserverbot.discord.enabled"],
        havingValue = "false",
        matchIfMissing = true,
    )
    fun noOpDiscordClientGateway(): DiscordClientGateway = NoOpDiscordClientGateway()
}
