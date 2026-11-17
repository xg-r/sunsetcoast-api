package kr.sunsetcoast.common.advice

import kr.sunsetcoast.controller.reservation.dto.response.ConflictBody
import kr.sunsetcoast.domain.reservation.service.ReservationConflictException
import io.github.oshai.kotlinlogging.KotlinLogging
import kr.sunsetcoast.common.response.ErrorResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.servlet.resource.NoResourceFoundException

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

    /** 직접 예약: 같은 객실·날짜에 예약이 있음. 화면이 겹치는 예약을 보여주고 "그래도 저장"을 물을 수 있게 목록을 같이 보낸다 */
    @ExceptionHandler(ReservationConflictException::class)
    fun handleReservationConflict(e: ReservationConflictException): ResponseEntity<ReservationConflictResponse> {
        logger.info { "[ReservationConflict] ${e.message}" }
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(ReservationConflictResponse(message = e.message ?: "겹치는 예약이 있습니다.", conflicts = e.conflicts.map { ConflictBody.from(it) }))
    }

    data class ReservationConflictResponse(
        val exception: String = "ReservationConflictException",
        val message: String,
        val conflicts: List<ConflictBody>,
    )

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

    /** 없는 경로. 500으로 감싸지 않고 404로 돌려준다 */
    @ExceptionHandler(NoResourceFoundException::class)
    fun handleNoResourceFound(e: NoResourceFoundException): ResponseEntity<ErrorResponse> {
        logger.debug { "[NoResourceFound] ${e.message}" }
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ErrorResponse(exception = "NoResourceFoundException", message = "없는 주소입니다."))
    }

    /** 그 주소에서 받지 않는 요청 (예: 조회만 되는 로그인 기록에 POST·DELETE) → 405 */
    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun handleMethodNotSupported(e: HttpRequestMethodNotSupportedException): ResponseEntity<ErrorResponse> {
        logger.warn { "[MethodNotSupported] ${e.method} ${e.message}" }
        return ResponseEntity
            .status(HttpStatus.METHOD_NOT_ALLOWED)
            .body(ErrorResponse(exception = "HttpRequestMethodNotSupportedException", message = "이 주소에서는 할 수 없는 작업입니다 (${e.method})."))
    }

    /** 코드에서 일부러 정한 상태 코드 (예: 운영에서 꺼 둔 개발용 로그인 → 404). 500으로 감싸지 않는다 */
    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatus(e: ResponseStatusException): ResponseEntity<ErrorResponse> {
        val status = HttpStatus.resolve(e.statusCode.value()) ?: HttpStatus.INTERNAL_SERVER_ERROR
        logger.debug { "[ResponseStatus] ${e.statusCode} ${e.reason}" }
        val message = e.reason ?: when (status) {
            HttpStatus.NOT_FOUND -> "없는 주소입니다."
            HttpStatus.UNAUTHORIZED -> "로그인이 필요합니다."
            HttpStatus.FORBIDDEN -> "권한이 없습니다."
            else -> status.reasonPhrase
        }
        return ResponseEntity.status(status).body(ErrorResponse(exception = "ResponseStatusException", message = message))
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
