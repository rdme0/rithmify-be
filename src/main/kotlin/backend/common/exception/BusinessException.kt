package backend.common.exception

import backend.common.exception.enums.ErrorCode
import org.springframework.core.NestedRuntimeException

open class BusinessException(val errorCode: ErrorCode, cause: Throwable? = null) :
        NestedRuntimeException(errorCode.message, cause)
