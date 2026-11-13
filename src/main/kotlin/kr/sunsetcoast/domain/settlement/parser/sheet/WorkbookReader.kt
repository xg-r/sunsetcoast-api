package kr.sunsetcoast.domain.settlement.parser.sheet

import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DateUtil
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.ByteArrayInputStream
import java.math.BigDecimal

/**
 * xls/xlsx를 확장자가 아닌 파일 내용으로 판별해서 읽는다.
 * (NOL 정산 파일은 확장자가 .xls지만 실제로는 xlsx, 떠나요 정산 파일은 진짜 xls)
 */
object WorkbookReader {
    fun readFirstSheet(bytes: ByteArray): RawSheet {
        WorkbookFactory.create(ByteArrayInputStream(bytes)).use { workbook ->
            val sheet = workbook.getSheetAt(0)
            val rows = sheet.mapNotNull { row ->
                if (row.lastCellNum < 0) return@mapNotNull null
                val cells = (0 until row.lastCellNum).map { i -> cellText(row.getCell(i)) }
                (row.rowNum + 1) to cells
            }
            return RawSheet(rows)
        }
    }

    private fun cellText(cell: Cell?): String {
        if (cell == null) return ""
        return when (cell.cellType) {
            CellType.FORMULA -> valueOf(cell, cell.cachedFormulaResultType)
            else -> valueOf(cell, cell.cellType)
        }.trim()
    }

    private fun valueOf(cell: Cell, type: CellType): String = when (type) {
        CellType.STRING -> cell.stringCellValue
        // 숫자 셀은 지수 표기(7.2217328E7) 없이 그대로 쓴다. 예약번호가 숫자 셀인 파일이 있다.
        CellType.NUMERIC ->
            if (DateUtil.isCellDateFormatted(cell)) cell.localDateTimeCellValue.toString()
            else BigDecimal.valueOf(cell.numericCellValue).stripTrailingZeros().toPlainString()
        CellType.BOOLEAN -> cell.booleanCellValue.toString()
        else -> ""
    }
}
