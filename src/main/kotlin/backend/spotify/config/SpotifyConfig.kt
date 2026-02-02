package backend.spotify.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "spotify.api")
data class SpotifyApiProperties(val baseUrl: String)

@ConfigurationProperties(prefix = "spotify.security")
data class SpotifySecurityProperties(
        val tokenUrl: String,
        val clientId: String,
        val clientSecret: String
)
