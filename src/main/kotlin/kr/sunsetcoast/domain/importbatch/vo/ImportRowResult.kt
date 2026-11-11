package kr.sunsetcoast.domain.importbatch.vo

/**
 * 원본 행 처리 결과. 선언 순서가 심각도 순서다 (뒤로 갈수록 확인 필요).
 */
enum class ImportRowResult {
    /** 저장할 금액이 없는 행 (예: 수수료 없는 취소) */
    SKIPPED,

    /** 이미 같은 값이 있어 건너뜀 */
    DUPLICATE,

    /** 새로 저장됨 */
    INSERTED,

    /** 같은 키의 기존 행과 금액이 달라 갱신됨 (이전 값은 revision으로 보존) */
    CHANGED,

    /** 객실 매핑 실패 - 관리자 확인 필요 */
    UNMAPPED,

    /** 파싱/검증 실패 */
    FAILED,
    ;

    val needsAttention: Boolean get() = this == CHANGED || this == UNMAPPED || this == FAILED

    /** 한 원본 행이 여러 정산 행을 만들 때(떠나요 연박) 대표 결과 */
    fun mergeWith(other: ImportRowResult): ImportRowResult = if (other.ordinal > ordinal) other else this
}
