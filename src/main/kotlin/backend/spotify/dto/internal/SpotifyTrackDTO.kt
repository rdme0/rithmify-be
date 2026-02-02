package backend.spotify.dto.internal

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class SpotifyTrackDTO(
        val id: String,
        val name: String,
        val artists: List<SpotifyArtistDTO>,
        val album: SpotifyAlbumDTO,
        val previewUrl: String?,
        val durationMs: Long
)
