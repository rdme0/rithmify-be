package backend.common.exception.enums

import backend.common.constant.Domain
import org.springframework.http.HttpStatus

enum class ErrorCode(
        private val domain: Domain,
        val status: HttpStatus,
        private val number: Int,
        val message: String
) {
    // Common
    INTERNAL_SERVER(Domain.COMMON, HttpStatus.INTERNAL_SERVER_ERROR, 1, "서버 내부 오류입니다."),
    INVALID_INPUT_VALUE(Domain.COMMON, HttpStatus.BAD_REQUEST, 1, "유효하지 않은 입력 값입니다."),
    BAD_DATA_SYNTAX(Domain.COMMON, HttpStatus.BAD_REQUEST, 2, "%s"),
    UNAUTHORIZED(Domain.COMMON, HttpStatus.UNAUTHORIZED, 1, "인증되지 않은 사용자 입니다."),

    // Auth
    USER_NOT_FOUND(Domain.AUTH, HttpStatus.NOT_FOUND, 1, "존재하지 않는 사용자입니다."),

    // TierList
    TIER_LIST_NOT_FOUND(Domain.TIERLIST, HttpStatus.NOT_FOUND, 1, "존재하지 않는 티어리스트입니다."),
    TIER_GROUP_NOT_FOUND(Domain.TIERLIST, HttpStatus.NOT_FOUND, 2, "존재하지 않는 티어 그룹입니다."),

    // Spotify
    SPOTIFY_API_ERROR(Domain.SPOTIFY, HttpStatus.BAD_GATEWAY, 1, "Spotify API 호출 중 오류가 발생했습니다.");

    fun getCode() = "${domain.name}_${status.value()}_%03d".format(number)
}
