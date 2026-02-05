package backend.spotify.client

import backend.common.exception.server.InternalServerException
import backend.common.util.retry
import backend.spotify.config.SpotifyApiProperties
import backend.spotify.config.SpotifySecurityProperties
import backend.spotify.dto.internal.*
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.resilience4j.ratelimiter.RateLimiter
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.awaitBody
import java.time.Duration

@Component
@EnableConfigurationProperties(SpotifySecurityProperties::class)
class SpotifyTokenManager(
    private val properties: SpotifySecurityProperties,
    private val redisTemplate: StringRedisTemplate
) {
    companion object {
        private const val REDIS_KEY_ACCESS_TOKEN = "spotify:access_token"
        private const val TOKEN_BUFFER_SECONDS = 60L
    }

    suspend fun getToken(): String {
        return redisTemplate.opsForValue().get(REDIS_KEY_ACCESS_TOKEN) ?: refreshToken()
    }

    private suspend fun refreshToken(): String {
        val client = WebClient.create()
        val response = client.post()
            .uri(properties.tokenUrl)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(
                BodyInserters.fromFormData("grant_type", "client_credentials")
                    .with("client_id", properties.clientId)
                    .with("client_secret", properties.clientSecret)
            )
            .retrieve()
            .awaitBody<Map<String, Any>>()

        val accessToken = response["access_token"] as? String
            ?: throw InternalServerException(
                IllegalStateException(
                    "Failed to retrieve access token from Spotify"
                )
            )

        val expiresIn = (response["expires_in"] as? Int)?.toLong() ?: 3600L
        val ttl = Duration.ofSeconds(expiresIn - TOKEN_BUFFER_SECONDS)

        redisTemplate.opsForValue().set(REDIS_KEY_ACCESS_TOKEN, accessToken, ttl)

        return accessToken
    }
}

@Component
@EnableConfigurationProperties(SpotifyApiProperties::class)
class SpotifyClient(
    private val tokenManager: SpotifyTokenManager,
    private val properties: SpotifyApiProperties,
    @Qualifier("spotifyRateLimiter")
    private val rateLimiter: RateLimiter
) {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    private val webClient = WebClient.builder().baseUrl(properties.baseUrl).build()

    /**
     * Execute a Spotify API call with rate limiting and retry logic Handles 429 Too Many Requests
     * with Retry-After header
     */
    private suspend fun <T> executeWithRetry(block: suspend () -> T): T {
        return retry(
            times = 3,
            retryCondition = { it is WebClientResponseException.TooManyRequests },
            extractDelay = { e ->
                if (e is WebClientResponseException.TooManyRequests) {
                    val retryAfter = e.headers.getFirst("Retry-After")?.toLongOrNull() ?: 2L
                    retryAfter * 1000
                } else {
                    null
                }
            }
        ) {
            // Wait for permission (blocking but safe within timeout)
            val start = System.currentTimeMillis()
            if (rateLimiter.acquirePermission(1)) {
                val waitTime = System.currentTimeMillis() - start
                if (waitTime > 1000) {
                    logger.warn {
                        "RateLimiter throttle active. Waited ${waitTime}ms for permission."
                    }
                }
                block()
            } else {
                throw IllegalStateException("Rate limiter timeout")
            }
        }
    }

    suspend fun searchArtist(query: String): SpotifySearchResponseDTO? {
        return webClient
            .get()
            .uri { uriBuilder ->
                uriBuilder
                    .path("/search")
                    .queryParam("q", query)
                    .queryParam("type", "artist")
                    .queryParam("limit", 30)
                    .build()
            }
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenManager.getToken()}")
            .retrieve()
            .awaitBody<SpotifySearchResponseDTO>()
    }

    suspend fun getArtistTopTracks(artistId: String, market: String = "KR"): List<SpotifyTrackDTO> {
        val result = webClient
            .get()
            .uri("/artists/$artistId/top-tracks?market=$market")
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenManager.getToken()}")
            .retrieve()
            .awaitBody<SpotifyTopTracksResponseDTO>()

        return result.tracks
    }

    suspend fun getArtist(artistId: String): SpotifyArtistDTO {
        return webClient
            .get()
            .uri("/artists/$artistId")
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenManager.getToken()}")
            .retrieve()
            .awaitBody<SpotifyArtistDTO>()
    }

    suspend fun searchTracks(query: String, limit: Int, offset: Int): SpotifySearchResponseDTO? {
        return webClient
            .get()
            .uri { uriBuilder ->
                uriBuilder
                    .path("/search")
                    .queryParam("q", query)
                    .queryParam("type", "track")
                    .queryParam("limit", limit)
                    .queryParam("offset", offset)
                    .build()
            }
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenManager.getToken()}")
            .retrieve()
            .awaitBody<SpotifySearchResponseDTO>()
    }

    suspend fun getArtistAlbums(artistId: String): List<SpotifyAlbumDTO> {
        val allAlbums = mutableListOf<SpotifyAlbumDTO>()
        var nextUrl: String? = "/artists/$artistId/albums?include_groups=album,single&limit=50"

        while (nextUrl != null) {
            val currentUrl = nextUrl

            val result = executeWithRetry {
                webClient
                    .get()
                    .uri(currentUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenManager.getToken()}")
                    .retrieve()
                    .awaitBody<SpotifyAlbumResultDTO>()
            }

            allAlbums.addAll(result.items)
            nextUrl = result.next?.substringAfter(properties.baseUrl)
        }

        return allAlbums
    }

    suspend fun getAlbumTracks(albumId: String): List<SpotifySimplifiedTrackDTO> {
        val allTracks = mutableListOf<SpotifySimplifiedTrackDTO>()
        var nextUrl: String? = "/albums/$albumId/tracks?limit=50"

        while (nextUrl != null) {
            val currentUrl = nextUrl

            val result = executeWithRetry {
                webClient
                    .get()
                    .uri(currentUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenManager.getToken()}")
                    .retrieve()
                    .awaitBody<SpotifyAlbumTrackResultDTO>()
            }

            allTracks.addAll(result.items)
            nextUrl = result.next?.substringAfter(properties.baseUrl)
        }

        return allTracks
    }

    /** Get multiple tracks by IDs (최대 50개) API 호출 수를 줄이기 위한 최적화 */
    suspend fun getTracksByIds(ids: List<String>): List<SpotifyTrackDTO> {
        require(ids.size <= 50) { "최대 50개의 트랙 ID만 조회 가능합니다" }
        if (ids.isEmpty()) return emptyList()

        return executeWithRetry {
            webClient
                .get()
                .uri { it.path("/tracks").queryParam("ids", ids.joinToString(",")).build() }
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenManager.getToken()}")
                .retrieve()
                .awaitBody<SpotifyTracksResponseDTO>()
                .tracks
        }
    }
}
