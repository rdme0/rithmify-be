package backend.common.constant

/** API에서 전달받는 정렬 키와 실제 정렬 로직을 연결하기 위한 추상 인터페이스 */
interface BaseSortField {
    val requestField: String
}
