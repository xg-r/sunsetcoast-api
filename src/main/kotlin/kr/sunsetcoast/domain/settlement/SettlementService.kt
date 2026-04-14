package kr.sunsetcoast.domain.settlement

import io.github.oshai.kotlinlogging.KotlinLogging
import kr.sunsetcoast.domain.settlement.normalizer.SettlementNormalizer
import kr.sunsetcoast.domain.settlement.vo.OtaPlatform
import kr.sunsetcoast.domain.settlement.vo.SettlementEntity
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile

private val logger = KotlinLogging.logger { }

@Service
class SettlementService(
    private val settlementNormalizer: SettlementNormalizer,
) {
    fun import(
        otaPlatform: OtaPlatform,
        files: List<MultipartFile>,
    ): List<SettlementEntity> {
        logger.info { "Importing CSV data for ${otaPlatform.name}" }
        val settlementDataList = settlementNormalizer.process(otaPlatform, files)
        return emptyList()
    }
}
