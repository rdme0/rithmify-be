package backend.spotify.dto.internal

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming

data class SpotifySearchResponseDTO(
        val artists: SpotifyArtistResultDTO? = null,
        val tracks: SpotifyTrackResultDTO? = null
)

data class SpotifyArtistResultDTO(val items: List<SpotifyArtistDTO>)

data class SpotifyTrackResultDTO(val items: List<SpotifyTrackDTO>)

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class SpotifyArtistDTO(
        val id: String,
        val name: String,
        val images: List<SpotifyImageDTO>,
        val genres: List<String> = emptyList()
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
data class SpotifyAlbumDTO(val id: String, val name: String, val images: List<SpotifyImageDTO>)

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class SpotifyImageDTO(val url: String, val height: Int?, val width: Int?)
