package backend.auth.exception

import backend.common.exception.BusinessException
import backend.common.exception.enums.ErrorCode

class MemberNotFoundException : BusinessException(ErrorCode.USER_NOT_FOUND)
