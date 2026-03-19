package kr.sunsetcoast.infrastructure.csv

import kr.sunsetcoast.domain.settlement.vo.OtaPlatform
import kr.sunsetcoast.infrastructure.csv.vo.CsvRows
import java.io.InputStream

interface CsvReader {
    fun supports(platform: OtaPlatform): Boolean
    fun read(inputStream: InputStream): CsvRows
}
