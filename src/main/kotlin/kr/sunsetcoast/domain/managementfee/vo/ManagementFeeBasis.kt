package kr.sunsetcoast.domain.managementfee.vo

/** 관리비 계산 근거 */
enum class ManagementFeeBasis {
    /** 단가대로 지급 */
    CHARGED,

    /** 취소 예약 → 0원 */
    CANCELLED,

    /** 같은 예약자가 같은 객실을 이어서 예약한 뒤쪽 예약 → 0원 (앞 예약에서 지급) */
    CONSECUTIVE,

    /** 확정 예약이지만 투숙한 밤이 없음 (입실일 모름, 입실 후 전부 취소) → 0원 */
    NO_STAY,

    /** 입실일에 맞는 단가가 없음 → 0원, 단가를 확인해야 한다 */
    NO_RATE,
}
