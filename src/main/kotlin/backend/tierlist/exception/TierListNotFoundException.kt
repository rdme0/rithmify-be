package backend.tierlist.exception

import backend.common.exception.BusinessException
import backend.common.exception.enums.ErrorCode

class TierListNotFoundException : BusinessException(ErrorCode.TIER_LIST_NOT_FOUND)
