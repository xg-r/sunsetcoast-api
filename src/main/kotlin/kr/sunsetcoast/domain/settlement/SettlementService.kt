package kr.sunsetcoast.domain.settlement

import io.github.oshai.kotlinlogging.KotlinLogging
import kr.sunsetcoast.domain.settlement.reader.SettlementFileReader
import kr.sunsetcoast.domain.settlement.vo.OtaPlatform
import kr.sunsetcoast.infrastructure.csv.vo.CsvRows
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile

private val logger = KotlinLogging.logger { }

@Service
class SettlementService(
    private val settlementFileReader: SettlementFileReader,
) {
    fun import(
        otaPlatform: OtaPlatform,
        files: List<MultipartFile>,
    ): CsvRows {
        logger.info { "Importing CSV data for ${otaPlatform.name}" }
        val readSettlementFile = settlementFileReader.read(otaPlatform, files)
        return readSettlementFile.toCsvRows()

        TODO("insert settlement data to Database!")
    }
}