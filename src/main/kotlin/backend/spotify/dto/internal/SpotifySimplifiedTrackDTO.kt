package backend.spotify.dto.internal

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class SpotifySimplifiedTrackDTO(
        val id: String,
        val name: String,
        val previewUrl: String?,
        val durationMs: Long,
        val artists: List<SpotifyArtistDTO>
)
