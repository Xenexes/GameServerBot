package de.xenexes.gameserverbot.config

import de.xenexes.gameserverbot.infrastructure.http.nitrado.CachingNitradoGateway
import de.xenexes.gameserverbot.infrastructure.http.nitrado.KtorNitradoGateway
import de.xenexes.gameserverbot.infrastructure.http.nitrado.NitradoProperties
import de.xenexes.gameserverbot.ports.outbound.NitradoGateway
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(NitradoProperties::class)
class NitradoConfiguration {
    @Bean
    fun nitradoGateway(properties: NitradoProperties): NitradoGateway {
        val httpGateway = KtorNitradoGateway(properties)
        return CachingNitradoGateway(httpGateway, properties.cache)
    }
}
