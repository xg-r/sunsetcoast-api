package kr.sunsetcoast.domain.settlement.reader.dto

import kr.sunsetcoast.domain.settlement.vo.OtaPlatform
import kr.sunsetcoast.infrastructure.csv.vo.CsvRows

data class SettlementFile(
    val platform: OtaPlatform,
    val ddnayoSettlementFile: DdnayoSettlementFile?,
) {
    fun toCsvRows(): CsvRows {
        return CsvRows(emptyList())
    }
}

data class DdnayoSettlementFile(
    val id: Long,
)
