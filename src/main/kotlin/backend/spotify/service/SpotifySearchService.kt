package backend.spotify.service

import backend.common.constant.AlbumSortField
import backend.spotify.client.SpotifyClient
import backend.spotify.dto.internal.SpotifyAlbumDTO
import backend.spotify.dto.response.AlbumResponse
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
        private const val CACHE_KEY_PREFIX = "spotify:v4:artist" // Versioned cache key (v4)
        private const val CACHE_TTL_HOURS = 24L
    }

    suspend fun searchArtist(query: String): List<ArtistSearchResponse> {
        val response = spotifyClient.searchArtist(query)

        val processed =
                response?.artists?.items?.map { artist ->
                    val images = artist.images ?: emptyList()

                    ArtistSearchResponse(
                            id = artist.id.trim(),
                            name = artist.name,
                            images =
                                    images.map {
                                        SpotifyImageResponse(
                                                url = it.url,
                                                height = it.height,
                                                width = it.width
                                        )
                                    },
                            genres = artist.genres ?: emptyList()
                    )
                }
                        ?: emptyList()

        logger.info { "After processing for '$query': ${processed.take(5).map { it.name }}" }

        return processed.verifyUniqueness("searchArtist") { it.id }
    }

    suspend fun getArtistTopTracks(artistId: String): List<TrackResponse> {
        val tracks = spotifyClient.getArtistTopTracks(artistId)

        val processed =
                tracks.distinctBy { it.id.trim() }.map { track ->
                    val artistName = track.artists.joinToString(", ") { it.name }
                    val images = track.album.images ?: emptyList()

                    TrackResponse(
                            id = track.id.trim(),
                            name = track.name,
                            artistName = artistName,
                            albumName = track.album.name,
                            imageUrl = images.firstOrNull()?.url,
                            releaseDate = track.album.releaseDate,
                            durationMs = track.durationMs
                    )
                }

        return processed.verifyUniqueness("getArtistTopTracks") { it.id }
    }

    suspend fun getArtistAlbums(artistId: String, pageable: Pageable): Page<AlbumResponse> {
        val cacheKey = "spotify:v4:artist:$artistId:albums"
        val cachedData = redisTemplate.opsForValue().get(cacheKey)

        val albums: List<SpotifyAlbumDTO> =
                if (cachedData != null) {
                    objectMapper.readValue(cachedData)
                } else {
                    val freshAlbums =
                            spotifyClient.getArtistAlbums(artistId).distinctBy { it.id.trim() }

                    redisTemplate
                            .opsForValue()
                            .set(
                                    cacheKey,
                                    objectMapper.writeValueAsString(freshAlbums),
                                    CACHE_TTL_HOURS,
                                    TimeUnit.HOURS
                            )
                    freshAlbums
                }

        // Map to Response DTO first, then sort
        val mappedAlbums =
                albums.map { album ->
                    val images = album.images ?: emptyList()

                    AlbumResponse(
                            id = album.id,
                            name = album.name,
                            images =
                                    images.map { img ->
                                        SpotifyImageResponse(img.url, img.height, img.width)
                                    },
                            releaseDate = album.releaseDate,
                            totalTracks = album.totalTracks ?: 0
                    )
                }

        // Dynamic Sorting Logic
        val sort = pageable.sort
        val comparator: Comparator<AlbumResponse> =
                if (!sort.isSorted) {
                    null
                } else {
                    sort
                            .mapNotNull { order ->
                                AlbumSortField.fromRequestKey(order.property)
                                        ?.toComparator(order.isDescending)
                            }
                            .reduceOrNull { acc, next -> acc.thenComparing(next) }
                }
                        ?: compareByDescending { it.releaseDate }

        val finalComparator = comparator.thenBy { it.id }
        val sortedAlbums = mappedAlbums.sortedWith(finalComparator)

        // In-memory pagination
        val page = pageable.pageNumber
        val size = pageable.pageSize
        val fromIndex = page * size
        val toIndex = minOf(fromIndex + size, sortedAlbums.size)

        if (fromIndex >= sortedAlbums.size) {
            return PageImpl(emptyList(), pageable, sortedAlbums.size.toLong())
        }

        val resultList = sortedAlbums.subList(fromIndex, toIndex)

        resultList.verifyUniqueness("getArtistAlbums") { it.id }

        return PageImpl(resultList, pageable, sortedAlbums.size.toLong())
    }

    suspend fun getAlbumTracks(albumId: String): List<TrackResponse> {
        // Cache Key for Album Tracks (Versioned - v4)
        val cacheKey = "spotify:v4:album:$albumId:tracks"
        val cachedTracks = redisTemplate.opsForValue().get(cacheKey)

        if (cachedTracks != null) {
            return objectMapper.readValue(cachedTracks)
        }

        // 1. Get IDs from Album (Lightweight)
        val simpleTracks = spotifyClient.getAlbumTracks(albumId)
        val trackIds = simpleTracks.map { it.id.trim() }.distinct()

        // 2. Bulk Fetch Details (Heavyweight)
        val fullTracks =
                if (trackIds.isNotEmpty()) {
                    trackIds.chunked(50).flatMap { ids -> spotifyClient.getTracksByIds(ids) }
                } else {
                    emptyList()
                }

        // 3. Final Deduplication, Stable Sorting & Mapping
        val response =
                fullTracks.distinctBy { it.id.trim() }.sortedBy { it.id }.map { track ->
                    TrackResponse(
                            id = track.id.trim(),
                            name = track.name,
                            artistName = track.artists.joinToString(", ") { it.name },
                            albumName = track.album.name,
                            imageUrl = track.album.images?.firstOrNull()?.url,
                            releaseDate = track.album.releaseDate,
                            durationMs = track.durationMs
                    )
                }

        val finalResponse = response.verifyUniqueness("getAlbumTracks") { it.id }

        // Cache result
        redisTemplate
                .opsForValue()
                .set(
                        cacheKey,
                        objectMapper.writeValueAsString(finalResponse),
                        CACHE_TTL_HOURS,
                        TimeUnit.HOURS
                )

        return finalResponse
    }

    private fun <T> List<T>.verifyUniqueness(
            methodName: String,
            idSelector: (T) -> String
    ): List<T> {
        val distinct = this.distinctBy { idSelector(it).trim() }
        if (distinct.size < this.size) {

            val duplicates =
                    this.groupBy { idSelector(it).trim() }.filter { it.value.size > 1 }.keys

            logger.error {
                "[$methodName] CRITICAL: Duplicates found even after internal processing: $duplicates"
            }
        }
        return distinct
    }
}
