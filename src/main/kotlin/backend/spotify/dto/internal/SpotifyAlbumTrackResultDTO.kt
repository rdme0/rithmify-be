package backend.spotify.dto.internal

data class SpotifyAlbumTrackResultDTO(
        val items: List<SpotifySimplifiedTrackDTO>,
        val next: String?
)
