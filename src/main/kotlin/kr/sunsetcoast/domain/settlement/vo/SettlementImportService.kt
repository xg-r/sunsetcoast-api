package kr.sunsetcoast.domain.settlement.vo

import kr.sunsetcoast.infrastructure.csv.CsvReaderRegistry
import org.springframework.stereotype.Service

@Service
class SettlementImportService(
    private val csvReaderRegistry: CsvReaderRegistry,
) {

}
