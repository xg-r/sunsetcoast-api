package kr.sunsetcoast.domain.settlement.parser

import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.ByteArrayOutputStream

object WorkbookFixtures {
    fun xlsx(rows: List<List<Any?>>): ByteArray = write(XSSFWorkbook(), rows)

    fun xls(rows: List<List<Any?>>): ByteArray = write(HSSFWorkbook(), rows)

    private fun write(workbook: Workbook, rows: List<List<Any?>>): ByteArray = workbook.use { wb ->
        val sheet = wb.createSheet("sheet1")
        rows.forEachIndexed { r, values ->
            val row = sheet.createRow(r)
            values.forEachIndexed { c, value ->
                when (value) {
                    null -> Unit
                    is Number -> row.createCell(c).setCellValue(value.toDouble())
                    else -> row.createCell(c).setCellValue(value.toString())
                }
            }
        }
        ByteArrayOutputStream().also { wb.write(it) }.toByteArray()
    }
}
