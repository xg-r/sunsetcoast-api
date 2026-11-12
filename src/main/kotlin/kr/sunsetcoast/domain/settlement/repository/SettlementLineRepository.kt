package kr.sunsetcoast.domain.settlement.repository

import kr.sunsetcoast.domain.settlement.entity.SettlementLineEntity
import kr.sunsetcoast.domain.settlement.vo.SettlementLineType
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface SettlementLineRepository : JpaRepository<SettlementLineEntity, Long> {
    fun findByReservationIdAndLineTypeAndStayDateAndSeq(
        reservationId: Long,
        lineType: SettlementLineType,
        stayDate: LocalDate,
        seq: Int,
    ): SettlementLineEntity?

    fun findByReservationId(reservationId: Long): List<SettlementLineEntity>

    fun findByReservationIdIn(reservationIds: Collection<Long>): List<SettlementLineEntity>
}
