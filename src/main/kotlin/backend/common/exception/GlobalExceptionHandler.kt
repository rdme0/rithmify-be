package backend.common.exception

import backend.common.exception.enums.ErrorCode
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

private val logger = KotlinLogging.logger {}

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(e: BusinessException): ResponseEntity<ErrorResponse> {
        logger.warn { "BusinessException: ${e.errorCode.message}" }
        val errorCode = e.errorCode
        val response = ErrorResponse(
            code = errorCode.getCode(),
            message = e.message ?: errorCode.message
        )
        return ResponseEntity.status(errorCode.status).body(response)
    }

    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception): ResponseEntity<ErrorResponse> {
        logger.error(e) { "Internal Server Error" }
        val errorCode = ErrorCode.INTERNAL_SERVER
        val response = ErrorResponse(code = errorCode.getCode(), message = errorCode.message)
        return ResponseEntity.status(errorCode.status).body(response)
    }
}

data class ErrorResponse(val code: String, val message: String)
