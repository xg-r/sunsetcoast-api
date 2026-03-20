package kr.sunsetcoast.infrastructure.csv.registry

import kr.sunsetcoast.domain.settlement.vo.OtaPlatform
import kr.sunsetcoast.infrastructure.csv.reader.CsvReader
import org.springframework.stereotype.Component

@Component
class CsvReaderRegistry(
    private val readers: List<CsvReader>
) {
    fun get(platform: OtaPlatform): CsvReader {
        return readers.firstOrNull { it.supports(platform) }
            ?: throw IllegalArgumentException("No CsvReader found for platform=$platform")
    }
}
