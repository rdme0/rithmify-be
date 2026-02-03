package backend.spotify.service

import backend.spotify.client.SpotifyClient
import backend.spotify.dto.internal.SpotifyAlbumDTO
import backend.spotify.dto.response.ArtistSearchResponse
import backend.spotify.dto.response.SpotifyImageResponse
import backend.spotify.dto.response.TrackResponse
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.concurrent.TimeUnit
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
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

    suspend fun getArtistAlbums(artistId: String, pageable: Pageable): Page<SpotifyAlbumDTO> {
        // Fetch all albums from client (pagination supported by client loop)
        val albums = spotifyClient.getArtistAlbums(artistId)

        // In-memory pagination
        val page = pageable.pageNumber
        val size = pageable.pageSize
        val fromIndex = page * size
        val toIndex = minOf(fromIndex + size, albums.size)

        if (fromIndex >= albums.size) {
            return PageImpl(emptyList(), pageable, albums.size.toLong())
        }

        return PageImpl(albums.subList(fromIndex, toIndex), pageable, albums.size.toLong())
    }

    suspend fun getAlbumTracks(albumId: String): List<TrackResponse> {
        // Cache Key for Album Tracks
        val cacheKey = "spotify:album:$albumId:tracks"
        val cachedTracks = redisTemplate.opsForValue().get(cacheKey)

        if (cachedTracks != null) {
            return objectMapper.readValue(cachedTracks)
        }

        // 1. Get IDs from Album (Lightweight)
        // Note: Client ensures all tracks are fetched (pagination loop inside client)
        val simpleTracks = spotifyClient.getAlbumTracks(albumId)
        val trackIds = simpleTracks.map { it.id }

        // 2. Bulk Fetch Details (Heavyweight) to get Images
        // Even though it's one album, using Bulk Fetch ensures specific track details are rich
        val fullTracks =
                if (trackIds.isNotEmpty()) {
                    // Split into chunks of 50 just in case album has > 50 tracks
                    trackIds.chunked(50).flatMap { ids -> spotifyClient.getTracksByIds(ids) }
                } else {
                    emptyList()
                }

        val response =
                fullTracks.map { track ->
                    TrackResponse(
                            id = track.id,
                            name = track.name,
                            artistName = track.artists.joinToString(", ") { it.name },
                            albumName = track.album.name,
                            imageUrl = track.album.images?.firstOrNull()?.url,
                            previewUrl = track.previewUrl,
                            durationMs = track.durationMs
                    )
                }

        // Cache result
        redisTemplate
                .opsForValue()
                .set(
                        cacheKey,
                        objectMapper.writeValueAsString(response),
                        CACHE_TTL_HOURS,
                        TimeUnit.HOURS
                )

        return response
    }
}
