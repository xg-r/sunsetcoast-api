package kr.sunsetcoast.domain.settlement.repository

import kr.sunsetcoast.domain.settlement.entity.SettlementLineEntity
import kr.sunsetcoast.domain.settlement.vo.SettlementLineType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDate

interface SettlementLineRepository : JpaRepository<SettlementLineEntity, Long> {
    fun findByReservationIdAndRoomIdAndLineTypeAndStayDateAndSeq(
        reservationId: Long,
        roomId: Long,
        lineType: SettlementLineType,
        stayDate: LocalDate,
        seq: Int,
    ): SettlementLineEntity?

    fun findByReservationId(reservationId: Long): List<SettlementLineEntity>

    fun findByReservationIdIn(reservationIds: Collection<Long>): List<SettlementLineEntity>

    @Query(
        """
        select new kr.sunsetcoast.domain.settlement.repository.BatchPropertyCount(l.importBatchId, l.room.property.id, count(l))
        from SettlementLineEntity l
        where l.importBatchId is not null
        group by l.importBatchId, l.room.property.id
        """
    )
    fun countByBatchAndProperty(): List<BatchPropertyCount>

    @Query(
        """
        select new kr.sunsetcoast.domain.settlement.repository.BatchSettledOn(l.importBatchId, max(l.settledOn))
        from SettlementLineEntity l
        where l.importBatchId is not null and l.settledOn is not null
        group by l.importBatchId
        """
    )
    fun maxSettledOnByBatch(): List<BatchSettledOn>
}

/** 업로드 파일별·업소별 정산 행 수 */
data class BatchPropertyCount(val batchId: Long, val propertyId: Long, val lines: Long)

/** 업로드 파일별 마지막 정산일 */
data class BatchSettledOn(val batchId: Long, val settledOn: LocalDate)
