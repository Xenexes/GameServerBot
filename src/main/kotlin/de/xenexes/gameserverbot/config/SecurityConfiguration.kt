package de.xenexes.gameserverbot.config

import de.xenexes.gameserverbot.ports.outbound.UserRepository
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService
import org.springframework.security.core.userdetails.User
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.server.SecurityWebFilterChain

@Configuration
@EnableWebFluxSecurity
@EnableConfigurationProperties(SecurityProperties::class)
class SecurityConfiguration {
    @Bean
    fun securityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain =
        http
            .csrf { it.disable() }
            .authorizeExchange { exchanges ->
                exchanges
                    .pathMatchers("/actuator/health", "/actuator/health/**")
                    .permitAll()
                    .pathMatchers("/api/jobs/**")
                    .hasAnyRole("ADMIN", "CRON_JOB")
                    .pathMatchers("/api/**")
                    .hasRole("ADMIN")
                    .anyExchange()
                    .denyAll()
            }.httpBasic(Customizer.withDefaults())
            .build()

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun userDetailsService(
        passwordEncoder: PasswordEncoder,
        userRepository: UserRepository,
    ): MapReactiveUserDetailsService {
        val users =
            userRepository.findAll().map { credentials ->
                User
                    .withUsername(credentials.username)
                    .password(passwordEncoder.encode(credentials.password))
                    .roles(*credentials.roles.toTypedArray())
                    .build()
            }
        return MapReactiveUserDetailsService(users)
    }
}
