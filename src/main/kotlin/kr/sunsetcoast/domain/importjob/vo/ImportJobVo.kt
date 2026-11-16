package kr.sunsetcoast.domain.importjob.vo

enum class ImportJobKind(val label: String, val link: String) {
    SETTLEMENT("정산 파일", "/settlements/history"),
    BANK("계좌 거래내역", "/bank"),
}

enum class ImportJobStatus {
    QUEUED,
    RUNNING,
    SUCCEEDED,
    FAILED,
    ;

    val finished: Boolean get() = this == SUCCEEDED || this == FAILED
}

/** 계좌 거래내역 작업 결과 */
data class BankJobResult(
    val uploadId: Long,
    val bankAccountId: Long,
    val totalRows: Int,
    val insertedRows: Int,
    val duplicateRows: Int,
    val balanceBreaks: List<String>,
)
