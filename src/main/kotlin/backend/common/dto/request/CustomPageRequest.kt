package backend.common.dto.request

data class CustomPageRequest(
        val sort: String? = null,
        val direction: String? = null,
        val page: Int? = null,
        val size: Int? = null
)
