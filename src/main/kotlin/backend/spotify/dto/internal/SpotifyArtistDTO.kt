package backend.spotify.dto.internal

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class SpotifyArtistDTO(
        val id: String,
        val name: String,
        val images: List<SpotifyImageDTO>? = null,
        val genres: List<String>? = null
)
