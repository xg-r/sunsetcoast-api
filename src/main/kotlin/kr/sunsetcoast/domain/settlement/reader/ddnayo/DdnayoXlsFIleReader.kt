package kr.sunsetcoast.domain.settlement.reader.ddnayo

import kr.sunsetcoast.domain.settlement.normalizer.dto.DdnayoSettlementDto
import kr.sunsetcoast.domain.settlement.reader.FileExtension
import kr.sunsetcoast.domain.settlement.reader.FileReader
import kr.sunsetcoast.domain.settlement.vo.OtaPlatform
import kr.sunsetcoast.domain.settlement.vo.SettlementData
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.Row
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile

@Component
class DdnayoXlsFIleReader : FileReader {
    private val dataFormatter = DataFormatter()

    override fun supports(platform: OtaPlatform, extension: FileExtension): Boolean {
        return platform == OtaPlatform.DDNAYO && extension == FileExtension.XLS
    }

    override fun read(multipartFile: MultipartFile): SettlementData {
        val fileName = extractFileName(multipartFile)
        val settlementFiles = readDto(multipartFile)
            .map { it.toSettlementFile() }

        return SettlementData(
            fileName = fileName,
            settlementFiles = settlementFiles,
        )
    }

    private fun readDto(multipartFile: MultipartFile): List<DdnayoSettlementDto> {
        return multipartFile.inputStream.use { inputStream ->
            HSSFWorkbook(inputStream).use { workbook ->
                val sheet = workbook.getSheetAt(0)
                val headerRow = sheet.getRow(0)
                    ?: throw IllegalArgumentException("헤더 행이 없습니다.")

                val headers = extractHeaders(headerRow)
                val firstRowNum = 1
                val lastRowNum = sheet.lastRowNum

                return (firstRowNum..lastRowNum)
                    .mapNotNull { rowIndex -> sheet.getRow(rowIndex) }
                    .map { row -> toRowMap(row, headers) }
                    .filterNot { row -> isBlankRow(row) }
                    .map { row -> DdnayoSettlementDto.from(row) }
            }
        }
    }

    private fun extractHeaders(headerRow: Row): List<String> {
        return (0 until headerRow.lastCellNum).map { cellIndex ->
            getCellValue(headerRow.getCell(cellIndex))
        }
    }

    private fun toRowMap(row: Row, headers: List<String>): Map<String, String> {
        val values = headers.indices.map { cellIndex ->
            getCellValue(row.getCell(cellIndex))
        }

        return headers.zip(values).toMap()
    }

    private fun isBlankRow(row: Map<String, String>): Boolean {
        return row.values.all { it.isBlank() }
    }

    private fun extractFileName(multipartFile: MultipartFile): String {
        return multipartFile.originalFilename
            ?: throw IllegalArgumentException("파일 이름이 없습니다.")
    }

    private fun getCellValue(cell: Cell?): String {
        return dataFormatter.formatCellValue(cell).trim()
    }
}
