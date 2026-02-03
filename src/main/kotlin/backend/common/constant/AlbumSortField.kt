package backend.common.constant

import backend.spotify.dto.response.AlbumResponse

/** 앨범 목록 조회 시 사용 가능한 정렬 필드 정의 */
enum class AlbumSortField(
        override val requestField: String,
        val comparator: Comparator<AlbumResponse>
) : BaseSortField {
    NAME("name", compareBy { it.name }),
    RELEASE_DATE("release_date", compareBy { it.releaseDate });

    companion object {
        fun fromRequestKey(requestKey: String): AlbumSortField? {
            return entries.find { it.requestField == requestKey }
        }
    }
}
