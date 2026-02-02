package backend.spotify.dto.internal

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class SpotifyImageDTO(val url: String, val height: Int?, val width: Int?)
