package kr.sunsetcoast.domain.settlement.parser

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kr.sunsetcoast.domain.settlement.parser.ddnayo.DdnayoSettlementParser
import kr.sunsetcoast.domain.settlement.parser.nol.NolSettlementParser
import kr.sunsetcoast.domain.settlement.parser.yeogi.YeogiSettlementParser
import kr.sunsetcoast.domain.settlement.vo.DdnayoFileContent
import kr.sunsetcoast.domain.settlement.vo.SettlementFileName
import java.io.File
import java.time.LocalDate

/**
 * 실제 플랫폼 정산 파일로 파서와 객실 매핑 시드(V16)를 검증한다.
 * 개인정보가 있는 원본 파일은 git에 없으므로 docs/202610/정산 폴더가 있는 로컬에서만 실행된다.
 */
class SampleSettlementFilesTest : FunSpec({
    val sampleDir = File("docs/202610/정산")
    // 객실 별칭 시드: V16(최초) + V35(운영 중 '객실 지정'으로 추가한 것)
    val aliasSeeds = listOf(
        File("src/main/resources/db/migration/V16__insert_room_aliases.sql"),
        File("src/main/resources/db/migration/V35__insert_room_aliases_added_in_operation.sql"),
    )

    test("샘플 정산 파일 전체가 오류 없이 파싱되고 모든 객실명이 매핑된다").config(enabled = sampleDir.isDirectory) {
        val parsers = SettlementFileParsers(
            listOf(DdnayoSettlementParser(), YeogiSettlementParser(), NolSettlementParser())
        )
        val aliases = Regex("SELECT '(\\w+)', '(.*)', id FROM rooms").findAll(aliasSeeds.joinToString("\n") { it.readText() })
            .map { it.groupValues[1] to it.groupValues[2].replace("''", "'") }
            .toSet()

        val errors = mutableListOf<String>()
        val unmapped = mutableSetOf<String>()
        val multiRoom = mutableListOf<String>()

        sampleDir.listFiles()!!.filter { it.isFile }.sortedBy { it.name }.forEach { file ->
            val parsed = parsers.parse(file.readBytes())
            val lines = parsed.rows.flatMap { it.lines }
            println("${file.name}: platform=${parsed.platform} rows=${parsed.rows.size} lines=${lines.size} " +
                "net=${lines.sumOf { it.amounts.net }}")

            parsed.rows.filter { it.error != null }.forEach { errors += "${file.name}#${it.rowNo}: ${it.error}" }
            lines.filter { (parsed.platform.name to it.roomName) !in aliases }.forEach {
                // 한 예약에 객실이 여러 개 묶인 행은 의도적으로 매핑하지 않는다 (관리자 확인 대상)
                if (it.roomName.contains(',')) multiRoom += "${file.name}: ${it.reservationNumber} ${it.stayDate} ${it.roomName}"
                else unmapped += "${parsed.platform}: ${it.roomName}"
            }
        }

        multiRoom.forEach { println("다중 객실(미매핑 처리): $it") }
        errors.shouldBeEmpty()
        unmapped.shouldBeEmpty()
        (sampleDir.listFiles()!!.count { it.isFile } > 0) shouldBe true
    }

    test("떠나요 파일 내용으로 추정한 계정·정산일이 파일명과 맞는다").config(enabled = sampleDir.isDirectory) {
        val parsers = SettlementFileParsers(listOf(DdnayoSettlementParser(), YeogiSettlementParser(), NolSettlementParser()))
        val files = sampleDir.listFiles()!!.filter { it.isFile && it.name.endsWith("_떠나요.xls") }
        files.forEach { file ->
            val content = DdnayoFileContent.of(parsers.parse(file.readBytes()).rows.map { it.payload })
            val name = SettlementFileName.parse(file.name, LocalDate.of(2026, 11, 13))
            println("${file.name}: $content")
            content.issuedOn shouldBe name.issuedOn
            content.businessNumber shouldBe "278-27-01574"
        }
        (files.size > 0) shouldBe true
    }
})
