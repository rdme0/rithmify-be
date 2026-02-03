package backend.spotify.dto.internal

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming

/** Spotify Core Domain DTOs 이 파일은 Spotify의 핵심 도메인 객체들(Artist, Track, Album, Image)을 포함합니다. */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class SpotifyArtistDTO(
        val id: String,
        val name: String,
        val images: List<SpotifyImageDTO>? = null,
        val genres: List<String>? = null
)

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class SpotifyTrackDTO(
        val id: String,
        val name: String,
        val artists: List<SpotifyArtistDTO>,
        val album: SpotifyAlbumDTO,
        val previewUrl: String?,
        val durationMs: Long
)

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class SpotifySimplifiedTrackDTO(
        val id: String,
        val name: String,
        val previewUrl: String?,
        val durationMs: Long,
        val artists: List<SpotifyArtistDTO>
)

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class SpotifyAlbumDTO(
        val id: String,
        val name: String,
        val images: List<SpotifyImageDTO>? = null
)

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class SpotifyImageDTO(val url: String, val height: Int?, val width: Int?)
