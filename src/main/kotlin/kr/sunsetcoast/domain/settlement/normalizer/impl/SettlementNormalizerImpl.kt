package kr.sunsetcoast.domain.settlement.normalizer.impl

import io.github.oshai.kotlinlogging.KotlinLogging
import kr.sunsetcoast.domain.settlement.normalizer.SettlementNormalizer
import kr.sunsetcoast.domain.settlement.reader.FileExtension
import kr.sunsetcoast.domain.settlement.reader.FileReader
import kr.sunsetcoast.domain.settlement.vo.OtaPlatform
import kr.sunsetcoast.domain.settlement.vo.SettlementData
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile

private val logger = KotlinLogging.logger {}

@Component
class SettlementNormalizerImpl(
    private val fileReaders: List<FileReader>,
) : SettlementNormalizer {
    override fun process(
        otaPlatform: OtaPlatform,
        files: List<MultipartFile>,
    ): List<SettlementData> {

        val settlementData = files.map {
            val extension =
                extractExtension(it.originalFilename ?: throw NoSuchElementException("file name should not be null"))
            val fileReader = getStrategy(otaPlatform, extension)
            fileReader.read(it)
        }

        return settlementData
    }

    fun extractExtension(fileName: String): FileExtension {
        return when (fileName.substringAfterLast('.', "").lowercase()) {
            "csv" -> FileExtension.CSV
            "tsv" -> FileExtension.TSV
            "xls" -> FileExtension.XLS
            "xlsx" -> FileExtension.XLSX
            else -> throw IllegalArgumentException("Unsupported extension")
        }
    }

    private fun getStrategy(
        platform: OtaPlatform,
        extension: FileExtension,
    ): FileReader {
        return fileReaders.firstOrNull { it.supports(platform, extension) }
            ?: throw IllegalArgumentException("No CsvReader found for platform=$platform")
    }
}
