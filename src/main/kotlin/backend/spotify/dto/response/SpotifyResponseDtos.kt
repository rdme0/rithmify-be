package backend.spotify.dto.response

data class ArtistSearchResponse(
        val id: String,
        val name: String,
        val imageUrl: String?,
        val genres: List<String>
)

data class TrackResponse(
        val id: String,
        val name: String,
        val artistName: String, // Simplified for UI
        val albumName: String,
        val albumImageUrl: String?,
        val previewUrl: String?,
        val durationMs: Long
)
