package kr.sunsetcoast.domain.settlement.reader

import kr.sunsetcoast.domain.settlement.reader.dto.SettlementFile
import kr.sunsetcoast.domain.settlement.vo.OtaPlatform
import org.springframework.web.multipart.MultipartFile

interface SettlementFileReader {
    fun read(
        otaPlatform: OtaPlatform,
        files: List<MultipartFile>,
    ): SettlementFile
}
