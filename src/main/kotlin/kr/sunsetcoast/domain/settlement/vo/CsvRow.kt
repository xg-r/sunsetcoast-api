package kr.sunsetcoast.domain.settlement.vo

data class CsvRow(
    private val values: Map<String, String>,
    val rowNumber: Int,
)
