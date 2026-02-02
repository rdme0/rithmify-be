package backend.spotify.dto.response

data class ArtistSearchResponse(
        val id: String,
        val name: String,
        val images: List<SpotifyImageResponse>,
        val genres: List<String>
)

data class SpotifyImageResponse(val url: String, val height: Int?, val width: Int?)

data class TrackResponse(
        val id: String,
        val name: String,
        val artistName: String,
        val albumName: String,
        val imageUrl: String?,
        val previewUrl: String?,
        val durationMs: Long
)
