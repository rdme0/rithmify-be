package backend.common.exception

import backend.common.exception.enums.ErrorCode
import org.springframework.core.NestedRuntimeException

abstract class BusinessException(
        val errorCode: ErrorCode,
        customMessage: String? = null,
        cause: Throwable? = null
) : NestedRuntimeException(customMessage ?: errorCode.message, cause)
