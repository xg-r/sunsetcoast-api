package kr.sunsetcoast.controller.settlement.dto.response

import kr.sunsetcoast.infrastructure.csv.vo.CsvRows

data class SettlementImportCsvResponseBody(
    val fileNames: List<String> = emptyList<String>(),
    val totalRowCount: Int = 0,
    val insertedRowCount: Int = 0,
    val durationMs: Double = 0.0,
) {
    companion object {
        fun fromCsvRows(csvRows: CsvRows): SettlementImportCsvResponseBody {
            return SettlementImportCsvResponseBody()
        }
    }
}
