package kr.sunsetcoast.domain.settlement.normalizer

import kr.sunsetcoast.domain.settlement.vo.OtaPlatform
import kr.sunsetcoast.domain.settlement.vo.SettlementData
import org.springframework.web.multipart.MultipartFile

interface SettlementNormalizer {
    fun process(
        otaPlatform: OtaPlatform,
        files: List<MultipartFile>,
    ): List<SettlementData>
}
