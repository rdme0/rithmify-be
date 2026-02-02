package backend.common.config

import backend.auth.service.CustomOAuth2UserService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
class SecurityConfig(private val customOAuth2UserService: CustomOAuth2UserService) {

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .authorizeHttpRequests { auth ->
                auth.requestMatchers("/api/spotify/**").permitAll()
                auth.requestMatchers("/login/**", "/oauth2/**").permitAll()
                auth.anyRequest().permitAll()
            }
            .oauth2Login { oauth2 ->
                oauth2.userInfoEndpoint { userInfo ->
                    userInfo.userService(customOAuth2UserService)
                }
                oauth2.defaultSuccessUrl(
                    "http://localhost:8080/",
                    true
                ) // Todo: Change to Frontend URL
            }

        return http.build()
    }
}
