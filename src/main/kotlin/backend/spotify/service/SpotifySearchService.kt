package backend.spotify.service

import backend.spotify.client.SpotifyClient
import backend.spotify.dto.response.ArtistSearchResponse
import backend.spotify.dto.response.SpotifyImageResponse
import backend.spotify.dto.response.TrackResponse
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service

@Service
class SpotifySearchService(
        private val spotifyClient: SpotifyClient,
        private val redisTemplate: StringRedisTemplate,
        private val objectMapper: ObjectMapper
) {
    companion object {
        private val logger = KotlinLogging.logger {}
        private const val CACHE_KEY_PREFIX = "spotify:artist"
        private const val CACHE_TTL_HOURS = 1L
    }

    suspend fun searchArtist(query: String): List<ArtistSearchResponse> {
        val response = spotifyClient.searchArtist(query)

        return response?.artists?.items?.map { artist ->
            ArtistSearchResponse(
                    id = artist.id,
                    name = artist.name,
                    images =
                            (artist.images ?: emptyList()).map { img ->
                                SpotifyImageResponse(
                                        url = img.url,
                                        height = img.height,
                                        width = img.width
                                )
                            },
                    genres = artist.genres ?: emptyList()
            )
        }
                ?: emptyList()
    }

    suspend fun getArtistTopTracks(artistId: String, requirePreview: Boolean): List<TrackResponse> {
        val tracks = spotifyClient.getArtistTopTracks(artistId)

        return tracks.filter { !requirePreview || !it.previewUrl.isNullOrEmpty() }.map { track ->
            TrackResponse(
                    id = track.id,
                    name = track.name,
                    artistName = track.artists.joinToString(", ") { it.name },
                    albumName = track.album.name,
                    imageUrl = (track.album.images ?: emptyList()).firstOrNull()?.url,
                    previewUrl = track.previewUrl,
                    durationMs = track.durationMs
            )
        }
    }

    suspend fun getArtistTracks(artistId: String, page: Int, size: Int): List<TrackResponse> {
        val cacheKey = "$CACHE_KEY_PREFIX:$artistId:all_tracks"
        val cachedTracks = redisTemplate.opsForValue().get(cacheKey)

        val allTracks: List<TrackResponse> =
                if (cachedTracks != null) {
                    logger.debug { "Cache hit for artist tracks: $artistId" }
                    objectMapper.readValue(
                            cachedTracks,
                            object : TypeReference<List<TrackResponse>>() {}
                    )
                } else {
                    logger.info {
                        "Cache miss for artist tracks: $artistId. Fetching from Spotify..."
                    }
                    val albums = spotifyClient.getArtistAlbums(artistId)

                    // Parallel fetch tracks from all albums
                    val tracks: List<TrackResponse> = coroutineScope {
                        albums
                                .map { album ->
                                    async {
                                        spotifyClient.getAlbumTracks(album.id).map { track ->
                                            TrackResponse(
                                                    id = track.id,
                                                    name = track.name,
                                                    artistName =
                                                            track.artists.joinToString(", ") {
                                                                it.name
                                                            },
                                                    albumName = album.name,
                                                    imageUrl =
                                                            (album.images ?: emptyList())
                                                                    .firstOrNull()
                                                                    ?.url,
                                                    previewUrl = track.previewUrl,
                                                    durationMs = track.durationMs
                                            )
                                        }
                                    }
                                }
                                .awaitAll()
                                .flatten()
                    }

                    // Deduplicate by Track ID (same track can be in multiple albums)
                    val distinctTracks = tracks.distinctBy { it.id }

                    // Cache the full list for 1 hour
                    redisTemplate
                            .opsForValue()
                            .set(
                                    cacheKey,
                                    objectMapper.writeValueAsString(distinctTracks),
                                    CACHE_TTL_HOURS,
                                    java.util.concurrent.TimeUnit.HOURS
                            )
                    distinctTracks
                }

        // In-memory pagination
        val fromIndex = page * size
        if (fromIndex >= allTracks.size) return emptyList()

        return allTracks.drop(fromIndex).take(size)
    }
}
