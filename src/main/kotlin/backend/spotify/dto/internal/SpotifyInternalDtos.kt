package backend.spotify.dto.internal

import com.fasterxml.jackson.annotation.JsonProperty

data class SpotifySearchResponseDTO(
    val artists: SpotifyArtistResultDTO? = null,
    val tracks: SpotifyTrackResultDTO? = null
)

data class SpotifyArtistResultDTO(val items: List<SpotifyArtistDTO>)

data class SpotifyTrackResultDTO(val items: List<SpotifyTrackDTO>)

data class SpotifyArtistDTO(
    val id: String,
    val name: String,
    val images: List<SpotifyImageDTO>,
    val genres: List<String> = emptyList()
)

data class SpotifyTrackDTO(
    val id: String,
    val name: String,
    val artists: List<SpotifyArtistDTO>,
    val album: SpotifyAlbumDTO,
    @JsonProperty("preview_url") val previewUrl: String?,
    @JsonProperty("duration_ms") val durationMs: Long
)

data class SpotifyAlbumDTO(val id: String, val name: String, val images: List<SpotifyImageDTO>)

data class SpotifyImageDTO(val url: String, val height: Int?, val width: Int?)
