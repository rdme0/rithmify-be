package backend.auth.exception

import backend.common.exception.BusinessException
import backend.common.exception.enums.ErrorCode

class UserNotFoundException : BusinessException(ErrorCode.USER_NOT_FOUND)
