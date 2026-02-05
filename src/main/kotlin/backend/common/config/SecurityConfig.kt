package backend.common.config

// OAuth2 소셜로그인 기능은 MVP에 포함되지 않음
// import backend.auth.service.CustomOAuth2UserService
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(UriSecurityConfig::class)
class SecurityConfig(
        // OAuth2 소셜로그인 기능은 MVP에 포함되지 않음
        // private val customOAuth2UserService: CustomOAuth2UserService,
        private val uriSecurityConfig: UriSecurityConfig
) {

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
                .csrf { it.disable() }
                .cors { it.configurationSource(corsConfigurationSource()) }
                .authorizeHttpRequests { auth ->
                    auth.requestMatchers("/api/spotify/**").permitAll()
                    // OAuth2 로그인 경로는 현재 사용하지 않음
                    // auth.requestMatchers("/login/**", "/oauth2/**").permitAll()
                    auth.anyRequest().permitAll()
                }
        // OAuth2 소셜로그인 기능은 MVP에 포함되지 않음
        // .oauth2Login { oauth2 ->
        //     oauth2.userInfoEndpoint { userInfo ->
        //         userInfo.userService(customOAuth2UserService)
        //     }
        //     oauth2.defaultSuccessUrl(
        //             uriSecurityConfig.defaultRedirectOrigin + uriSecurityConfig.successPath,
        //             true
        //     )
        // }

        return http.build()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        configuration.allowedOriginPatterns = uriSecurityConfig.allowedFrontEndOrigins
        configuration.allowedMethods =
                mutableListOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        configuration.allowedHeaders = mutableListOf("*")
        configuration.allowCredentials = true
        configuration.maxAge = 3600L

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }
}
