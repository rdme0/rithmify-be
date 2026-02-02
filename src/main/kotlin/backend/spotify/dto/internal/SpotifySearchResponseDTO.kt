package backend.spotify.dto.internal

data class SpotifySearchResponseDTO(
        val artists: SpotifyArtistResultDTO? = null,
        val tracks: SpotifyTrackResultDTO? = null
)
