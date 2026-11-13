package kr.sunsetcoast.domain.settlement.repository

import kr.sunsetcoast.domain.reservation.vo.OtaPlatform
import kr.sunsetcoast.domain.settlement.entity.SettlementWeekCheckEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface SettlementWeekCheckRepository : JpaRepository<SettlementWeekCheckEntity, Long> {
    fun findBySourceAndAccountAndWeekStart(source: OtaPlatform, account: String, weekStart: LocalDate): SettlementWeekCheckEntity?
}
