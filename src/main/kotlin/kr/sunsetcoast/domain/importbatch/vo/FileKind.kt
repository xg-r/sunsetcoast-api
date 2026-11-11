package kr.sunsetcoast.domain.importbatch.vo

enum class FileKind {
    /** 정산 파일 (금액 기준) */
    SETTLEMENT,

    /** 예약 파일 (과거 데이터 적재용) */
    RESERVATION,
}
