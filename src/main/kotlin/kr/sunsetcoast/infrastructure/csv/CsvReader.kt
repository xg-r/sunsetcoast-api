package kr.sunsetcoast.infrastructure.csv

import kr.sunsetcoast.infrastructure.csv.vo.CsvRows

interface CsvReader {
    fun read(inputStream: java.io.InputStream): CsvRows
}
