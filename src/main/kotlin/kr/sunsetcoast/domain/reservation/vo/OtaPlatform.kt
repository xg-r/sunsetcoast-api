package kr.sunsetcoast.domain.reservation.vo

/**
 * 데이터 출처(파일을 내려받은 플랫폼). 고객이 실제 예약한 곳은 [SalesChannel].
 */
enum class OtaPlatform {
    DDNAYO, YEOGI, NOL,
    ;
}
