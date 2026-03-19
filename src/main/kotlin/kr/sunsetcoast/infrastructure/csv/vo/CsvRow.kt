package kr.sunsetcoast.infrastructure.csv.vo

data class CsvRow(
    private val values: Map<String, String>,
    val rowNumber: Int,
)
