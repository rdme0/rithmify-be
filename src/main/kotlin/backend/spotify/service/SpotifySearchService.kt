package backend.spotify.service

import backend.common.constant.TrackSortField
import backend.spotify.client.SpotifyClient
import backend.spotify.dto.internal.SpotifyAlbumDTO
import backend.spotify.dto.response.ArtistSearchResponse
import backend.spotify.dto.response.SpotifyImageResponse
import backend.spotify.dto.response.TrackResponse
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis

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

    suspend fun getArtistTracks(artistId: String, pageable: Pageable): Page<TrackResponse> {
        val cacheKey = "$CACHE_KEY_PREFIX:$artistId:all_tracks"
        val cachedTracks = redisTemplate.opsForValue().get(cacheKey)

        val allTracks: List<TrackResponse> =
            if (cachedTracks != null) {
                logger.debug { "Cache hit for artist tracks: $artistId" }
                objectMapper.readValue<List<TrackResponse>>(cachedTracks)
            } else {
                val totalTime = measureTimeMillis {
                    logger.info {
                        "Cache miss for artist tracks: $artistId. Fetching from Spotify..."
                    }
                }

                var albums: List<SpotifyAlbumDTO>
                val albumTime = measureTimeMillis {
                    albums = spotifyClient.getArtistAlbums(artistId)
                }
                logger.info { "Fetched ${albums.size} albums in ${albumTime}ms" }

                // 1. Collect all Track IDs from albums in parallel (ID only)
                // Throttling: Process albums in chunks of 20 to prevent 429 bursts
                var trackIds: List<String> = emptyList()
                val idCollectionTime = measureTimeMillis {
                    trackIds = albums.chunked(20)
                        .flatMapIndexed { index, batch ->
                            logger.info {
                                "Processing album chunk ${index + 1}/${(albums.size + 19) / 20} (size: ${batch.size})..."
                            }
                            coroutineScope {
                                batch
                                    .map { album ->
                                        async {
                                            spotifyClient.getAlbumTracks(
                                                album.id
                                            )
                                                .map { it.id }
                                        }
                                    }
                                    .awaitAll()
                                    .flatten()
                            }
                        }
                        .distinct() // Deduplicate early (at ID level)
                }
                logger.info {
                    "Collected ${trackIds.size} unique track IDs in ${idCollectionTime}ms"
                }

                // 2. Fetch full track details in chunks of 50 (Bulk API)
                // This ensures we get full album info (including images) and reduce API calls
                var distinctTracks: List<TrackResponse> = emptyList()
                val detailsFetchTime = measureTimeMillis {
                    distinctTracks = coroutineScope {
                        trackIds.chunked(50)
                            .mapIndexed { index, chunk ->
                                if (index % 5 == 0)
                                    logger.info {
                                        "Fetching track details chunk ${index + 1}/${(trackIds.size + 49) / 50}..."
                                    }
                                async {
                                    spotifyClient.getTracksByIds(chunk).map { track ->
                                        TrackResponse(
                                            id = track.id,
                                            name = track.name,
                                            artistName =
                                                track.artists.joinToString(", ") {
                                                    it.name
                                                },
                                            albumName = track.album.name,
                                            imageUrl =
                                                track.album.images?.firstOrNull()
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
                }
                logger.info {
                    "Fetched full details for ${distinctTracks.size} tracks in ${detailsFetchTime}ms"
                }

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
        val sortedTracks =
            if (pageable.sort.isSorted) {
                val sortOrder = pageable.sort.first()
                val sortField = TrackSortField.fromRequestKey(sortOrder.property)

                if (sortField != null) {
                    val comparator =
                        if (sortOrder.isDescending) {
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
