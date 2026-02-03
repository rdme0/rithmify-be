package backend.common.exception

class InvalidPageableFieldException(val field: String, val value: String) :
        IllegalArgumentException("Invalid pageable field: $field = $value")
