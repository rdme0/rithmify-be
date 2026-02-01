package backend.spotify.client

import backend.spotify.config.SpotifyApiProperties
import backend.spotify.config.SpotifySecurityProperties
import backend.spotify.dto.internal.SpotifySearchResponseDTO
import backend.spotify.dto.internal.SpotifyTrackDTO
import backend.spotify.dto.internal.SpotifyTrackResultDTO
import java.time.Duration
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody

@Component
@EnableConfigurationProperties(SpotifySecurityProperties::class)
class SpotifyTokenManager(
        private val properties: SpotifySecurityProperties,
        private val redisTemplate: StringRedisTemplate
) {
    private val mutex = Mutex()

    companion object {
        private const val REDIS_KEY_ACCESS_TOKEN = "spotify:access_token"
        private const val TOKEN_BUFFER_SECONDS = 60L
    }

    suspend fun getToken(): String {
        // 1. Try Redis
        redisTemplate.opsForValue().get(REDIS_KEY_ACCESS_TOKEN)?.let {
            return it
        }

        // 2. Refresh with Mutex
        return mutex.withLock {
            // Double-check
            redisTemplate.opsForValue().get(REDIS_KEY_ACCESS_TOKEN)?.let {
                return@withLock it
            }

            refreshToken()
        }
    }

    private suspend fun refreshToken(): String {
        val client = WebClient.create()
        val response =
                client.post()
                        .uri(properties.tokenUrl)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .body(
                                BodyInserters.fromFormData("grant_type", "client_credentials")
                                        .with("client_id", properties.clientId)
                                        .with("client_secret", properties.clientSecret)
                        )
                        .retrieve()
                        .awaitBody<Map<String, Any>>()

        val accessToken =
                response["access_token"] as? String
                        ?: error("Failed to retrieve access token from Spotify")

        val expiresIn = (response["expires_in"] as? Int)?.toLong() ?: 3600L
        val ttl = Duration.ofSeconds(expiresIn - TOKEN_BUFFER_SECONDS)

        // 3. Save to Redis
        redisTemplate.opsForValue().set(REDIS_KEY_ACCESS_TOKEN, accessToken, ttl)

        return accessToken
    }
}

@Component
@EnableConfigurationProperties(SpotifyApiProperties::class)
class SpotifyClient(
        private val tokenManager: SpotifyTokenManager,
        private val properties: SpotifyApiProperties
) {
    private val webClient = WebClient.builder().baseUrl(properties.baseUrl).build()

    suspend fun searchArtist(query: String): SpotifySearchResponseDTO? {
        return webClient
                .get()
                .uri { uriBuilder ->
                    uriBuilder
                            .path("/search")
                            .queryParam("q", query)
                            .queryParam("type", "artist")
                            .queryParam("limit", 10)
                            .build()
                }
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenManager.getToken()}")
                .retrieve()
                .awaitBody<SpotifySearchResponseDTO>()
    }

    suspend fun getArtistTopTracks(artistId: String, market: String = "KR"): List<SpotifyTrackDTO> {
        val result =
                webClient
                        .get()
                        .uri("/artists/$artistId/top-tracks?market=$market")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenManager.getToken()}")
                        .retrieve()
                        .awaitBody<SpotifyTrackResultDTO>()

        return result.items
    }
}
