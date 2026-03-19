package kr.sunsetcoast.infrastructure.csv

interface CsvReader {
    fun read(inputStream: java.io.InputStream): List<Map<String, String>>
}
