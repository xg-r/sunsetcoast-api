package kr.sunsetcoast.common.advice

import io.github.oshai.kotlinlogging.KotlinLogging
import kr.sunsetcoast.common.response.ErrorResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

private val logger = KotlinLogging.logger {}

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(
        e: IllegalArgumentException,
    ): ResponseEntity<ErrorResponse> {
        logger.warn(e) { "[IllegalArgumentException] ${e.message}" }

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                ErrorResponse(
                    exception = e::class.simpleName ?: "IllegalArgumentException",
                    message = e.message ?: "잘못된 요청입니다.",
                )
            )
    }

    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalStateException(
        e: IllegalStateException,
    ): ResponseEntity<ErrorResponse> {
        logger.warn(e) { "[IllegalStateException] ${e.message}" }

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                ErrorResponse(
                    exception = e::class.simpleName ?: "IllegalStateException",
                    message = e.message ?: "잘못된 상태입니다.",
                )
            )
    }

    @ExceptionHandler(Exception::class)
    fun handleException(
        e: Exception,
    ): ResponseEntity<ErrorResponse> {
        logger.error(e) { "[Exception] unexpected error" }

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(
                ErrorResponse(
                    exception = e::class.simpleName ?: "Exception",
                    message = e.message ?: "서버 내부 오류가 발생했습니다.",
                )
            )
    }
}
