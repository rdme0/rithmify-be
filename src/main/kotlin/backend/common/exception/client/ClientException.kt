package backend.common.exception.client

import backend.common.exception.BusinessException
import backend.common.exception.enums.ErrorCode

abstract class ClientException(errorCode: ErrorCode) : BusinessException(errorCode)
