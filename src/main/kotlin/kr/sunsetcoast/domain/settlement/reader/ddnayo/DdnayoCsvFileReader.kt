package kr.sunsetcoast.domain.settlement.reader.ddnayo

import kr.sunsetcoast.domain.settlement.reader.FileExtension
import kr.sunsetcoast.domain.settlement.reader.FileReader
import kr.sunsetcoast.domain.settlement.vo.OtaPlatform
import kr.sunsetcoast.domain.settlement.vo.SettlementData
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile

@Component
class DdnayoCsvFileReader : FileReader {
    override fun supports(platform: OtaPlatform, extension: FileExtension): Boolean =
        platform == OtaPlatform.DDNAYO && extension == FileExtension.CSV

    override fun read(multipartFile: MultipartFile): SettlementData {
        TODO("not yet implement")
    }
}
