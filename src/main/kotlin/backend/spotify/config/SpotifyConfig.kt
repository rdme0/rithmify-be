package backend.spotify.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "spotify.api")
data class SpotifyApiProperties(val baseUrl: String)

@ConfigurationProperties(prefix = "spring.security.oauth2.client.registration.spotify")
data class SpotifySecurityProperties(
    val clientId: String,
    val clientSecret: String,
    val tokenUrl: String = "https://accounts.spotify.com/api/token"
)
