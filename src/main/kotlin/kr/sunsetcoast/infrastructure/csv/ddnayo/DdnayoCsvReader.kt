package kr.sunsetcoast.infrastructure.csv.ddnayo

import io.github.oshai.kotlinlogging.KotlinLogging
import kr.sunsetcoast.domain.settlement.vo.OtaPlatform
import kr.sunsetcoast.infrastructure.csv.CsvReader
import kr.sunsetcoast.infrastructure.csv.vo.CsvRows
import org.springframework.stereotype.Component
import java.io.InputStream

private val logger = KotlinLogging.logger {}

@Component
class DdnayoCsvReader : CsvReader {
    override fun supports(platform: OtaPlatform): Boolean = platform == OtaPlatform.DDNAYO

    override fun read(inputStream: InputStream): CsvRows {
        logger.info { "Start Reading DDNAYO CSV rows..." }
        TODO("Not yet implemented")
    }
}
