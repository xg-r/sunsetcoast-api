package kr.sunsetcoast.domain.settlement.reader.dto

import kr.sunsetcoast.domain.settlement.vo.OtaPlatform
import kr.sunsetcoast.infrastructure.csv.vo.CsvRows

data class SettlementFile(
    val otaPlatform: OtaPlatform,
    val ddnayoSettlementFile: DdnayoSettlementFile?,
    val yeogiSettlementFile: YeogiSettlementFile?,
    val nolSettlementFile: NolSettlementFile?,
) {
    fun toCsvRows(): CsvRows {
        return CsvRows(emptyList())
    }
}

data class DdnayoSettlementFile(
    val id: Long,
)

data class YeogiSettlementFile(
    val id: Long,
)

data class NolSettlementFile(
    val id: Long,
)
