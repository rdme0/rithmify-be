package backend.spotify.client

import backend.common.exception.server.InternalServerException
import backend.spotify.config.SpotifyApiProperties
import backend.spotify.config.SpotifySecurityProperties
import backend.spotify.dto.internal.*
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
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
                ?: throw InternalServerException(
                    IllegalStateException(
                        "Failed to retrieve access token from Spotify"
                    )
                )

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

            val result = webClient
                .get()
                .uri(currentUrl)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenManager.getToken()}")
                .retrieve()
                .awaitBody<SpotifyAlbumResultDTO>()

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

            val result = webClient
                .get()
                .uri(currentUrl)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenManager.getToken()}")
                .retrieve()
                .awaitBody<SpotifyAlbumTrackResultDTO>()

            allTracks.addAll(result.items)
            nextUrl = result.next?.substringAfter(properties.baseUrl)
        }
        return allTracks
    }
}
