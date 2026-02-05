package backend.common.exception

import backend.common.exception.enums.ErrorCode

class InvalidPageableFieldException(field: String, value: String) :
        BusinessException(
                ErrorCode.INVALID_PAGEABLE_FIELD,
                ErrorCode.INVALID_PAGEABLE_FIELD.message.format("$field='$value'")
        )
