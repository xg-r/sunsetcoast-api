package kr.sunsetcoast.infrastructure.csv.facade

import kr.sunsetcoast.domain.settlement.reader.SettlementFileReader
import kr.sunsetcoast.domain.settlement.reader.dto.SettlementFile
import kr.sunsetcoast.domain.settlement.vo.OtaPlatform
import kr.sunsetcoast.infrastructure.csv.registry.CsvReaderRegistry
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile

@Component
class CsvSettlementFileReader(
    private val csvReaderRegistry: CsvReaderRegistry,
) : SettlementFileReader {
    override fun read(
        otaPlatform: OtaPlatform,
        files: List<MultipartFile>,
    ): SettlementFile {
        val csvReader = csvReaderRegistry.get(otaPlatform)
        TODO("not yet implements")
    }
}
