package kr.sunsetcoast.domain.settlement.reader.impl

import kr.sunsetcoast.domain.settlement.reader.FileExtension
import kr.sunsetcoast.domain.settlement.reader.FileReader
import kr.sunsetcoast.domain.settlement.vo.OtaPlatform
import kr.sunsetcoast.domain.settlement.vo.SettlementData
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile

@Component
class NolCsvFileReader : FileReader {
    override fun supports(platform: OtaPlatform, extension: FileExtension): Boolean =
        platform == OtaPlatform.NOL && extension == FileExtension.CSV

    override fun read(multipartFile: MultipartFile): SettlementData {
        TODO("not yet implement")
    }
}
