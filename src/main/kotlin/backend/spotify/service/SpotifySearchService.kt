package backend.spotify.service

import backend.common.constant.TrackSortField
import backend.spotify.client.SpotifyClient
import backend.spotify.dto.response.ArtistSearchResponse
import backend.spotify.dto.response.SpotifyImageResponse
import backend.spotify.dto.response.TrackResponse
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

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
                images = (artist.images ?: emptyList()).map { img ->
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

    suspend fun getArtistTracks(
        artistId: String,
        pageable: Pageable
    ): Page<TrackResponse> {
        val cacheKey = "$CACHE_KEY_PREFIX:$artistId:all_tracks"
        val cachedTracks = redisTemplate.opsForValue().get(cacheKey)

        val allTracks: List<TrackResponse> = if (cachedTracks != null) {
            logger.debug { "Cache hit for artist tracks: $artistId" }
            objectMapper.readValue<List<TrackResponse>>(cachedTracks)
        } else {
            logger.info {
                "Cache miss for artist tracks: $artistId. Fetching from Spotify..."
            }
            val albums = spotifyClient.getArtistAlbums(artistId)

            // Process albums in chunks to avoid Spotify rate limits (429 errors)
            val tracks = mutableListOf<TrackResponse>()
            val chunkSize = 5 // Process 5 albums at a time

            albums.chunked(chunkSize).forEach { albumChunk ->
                val chunkTracks: List<TrackResponse> = coroutineScope {
                    albumChunk
                        .map { album ->
                            async {
                                spotifyClient.getAlbumTracks(album.id).map { track ->
                                    TrackResponse(
                                        id = track.id,
                                        name = track.name,
                                        artistName = track.artists.joinToString(", ") {
                                            it.name
                                        },
                                        albumName = album.name,
                                        imageUrl = (album.images ?: emptyList())
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
                tracks.addAll(chunkTracks)

                // Small delay between chunks to respect rate limits
                if (albumChunk != albums.chunked(chunkSize).last()) {
                    delay(100)
                }
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
                    TimeUnit.HOURS
                )
            distinctTracks
        }

        // Apply sorting if specified
        val sortedTracks = if (pageable.sort.isSorted) {
            val sortOrder = pageable.sort.first()
            val sortField = TrackSortField.fromRequestKey(
                sortOrder.property
            )

            if (sortField != null) {
                val comparator = if (sortOrder.isDescending) {
                    sortField.comparator.reversed()
                } else {
                    sortField.comparator
                }
                allTracks.sortedWith(comparator)
            } else {
                allTracks
            }
        } else {
            allTracks
        }

        // In-memory pagination
        val page = pageable.pageNumber
        val size = pageable.pageSize
        val fromIndex = page * size
        val toIndex = minOf(fromIndex + size, sortedTracks.size)

        // Return empty page if out of bounds
        if (fromIndex >= sortedTracks.size) {
            return PageImpl<TrackResponse>(emptyList(), pageable, sortedTracks.size.toLong())
        }

        val pageContent = sortedTracks.subList(fromIndex, toIndex)
        return PageImpl<TrackResponse>(pageContent, pageable, sortedTracks.size.toLong())
    }
}
