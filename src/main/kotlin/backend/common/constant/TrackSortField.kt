package backend.common.constant

import backend.spotify.dto.response.TrackResponse

enum class TrackSortField(
        override val requestField: String,
        val comparator: Comparator<TrackResponse>
) : BaseSortField {
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
