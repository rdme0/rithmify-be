package backend.common.converter

import backend.common.dto.request.CustomPageRequest
import backend.common.exception.InvalidPageableFieldException
import org.springframework.core.convert.converter.Converter
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort

class CustomPageRequestToPageableConverter(
        private val allowedFields: List<backend.common.constant.BaseSortField>
) : Converter<CustomPageRequest, Pageable> {

    override fun convert(request: CustomPageRequest): Pageable {
        val page = (request.page ?: 1) - 1

        if (page < 0) {
            throw InvalidPageableFieldException("page", "page는 1보다 작을 수 없습니다.")
        }

        val size = request.size ?: 20

        if (size !in 1..100) {
            throw InvalidPageableFieldException("size", "size는 1 ~ 100 사이여야 합니다.")
        }

        val directionString = request.direction ?: "asc"
        val requestFieldString = request.sort

        // If no sort field specified, return unsorted pageable
        if (requestFieldString == null) {
            return PageRequest.of(page, size)
        }

        val direction: Sort.Direction =
                try {
                    Sort.Direction.fromString(directionString.uppercase())
                } catch (_: IllegalArgumentException) {
                    throw InvalidPageableFieldException("direction", directionString)
                }

        val sortField: backend.common.constant.BaseSortField =
                allowedFields.find { it.requestField == requestFieldString }
                        ?: throw InvalidPageableFieldException("sort", requestFieldString)

        // For in-memory sorting, we'll use the comparator from TrackSortField
        // But we still return Pageable with Sort for API consistency
        val order = Sort.Order(direction, sortField.requestField)
        return PageRequest.of(page, size, Sort.by(order))
    }
}
