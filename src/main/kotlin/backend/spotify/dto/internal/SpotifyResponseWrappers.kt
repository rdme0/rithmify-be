package backend.spotify.dto.internal

/** Spotify API Response Wrapper DTOs 이 파일은 Spotify API 응답을 감싸는 작은 wrapper 클래스들을 포함합니다. */
data class SpotifySearchResponseDTO(
    val artists: SpotifyArtistResultDTO? = null,
    val tracks: SpotifyTrackResultDTO? = null
)

data class SpotifyArtistResultDTO(val items: List<SpotifyArtistDTO>)

data class SpotifyTrackResultDTO(val items: List<SpotifyTrackDTO>)

data class SpotifyTopTracksResponseDTO(val tracks: List<SpotifyTrackDTO>)

data class SpotifyAlbumResultDTO(val items: List<SpotifyAlbumDTO>, val next: String?)

data class SpotifyAlbumTrackResultDTO(
    val items: List<SpotifySimplifiedTrackDTO>,
    val next: String?
)

data class SpotifyTracksResponseDTO(val tracks: List<SpotifyTrackDTO>)
