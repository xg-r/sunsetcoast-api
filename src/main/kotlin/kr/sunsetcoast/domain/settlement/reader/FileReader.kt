package kr.sunsetcoast.domain.settlement.reader

import kr.sunsetcoast.domain.settlement.vo.OtaPlatform
import kr.sunsetcoast.domain.settlement.vo.SettlementData
import org.springframework.web.multipart.MultipartFile

interface FileReader {
    fun supports(platform: OtaPlatform, extension: FileExtension): Boolean
    fun read(multipartFile: MultipartFile): SettlementData
}
