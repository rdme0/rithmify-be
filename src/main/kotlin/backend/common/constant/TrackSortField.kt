package backend.common.constant

enum class TrackSortField(
        val requestField: String,
        val comparator: Comparator<backend.spotify.dto.response.TrackResponse>
) {
    NAME("name", compareBy { it.name }),
    DURATION("duration", compareBy { it.durationMs }),
    ALBUM_NAME("album", compareBy { it.albumName }),
    ARTIST_NAME("artist", compareBy { it.artistName });

    companion object {
        fun fromRequestKey(requestKey: String): TrackSortField? {
            return entries.find { it.requestField == requestKey }
        }
    }
}
