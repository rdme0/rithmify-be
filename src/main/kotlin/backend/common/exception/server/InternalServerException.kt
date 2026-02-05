package backend.common.exception.server

import backend.common.exception.BusinessException
import backend.common.exception.enums.ErrorCode

class InternalServerException(override val cause: Throwable) :
        BusinessException(ErrorCode.INTERNAL_SERVER, cause = cause)
