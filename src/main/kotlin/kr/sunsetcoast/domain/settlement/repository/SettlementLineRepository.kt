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

    /** 숙박일 월·업소·플랫폼·행 유형별 합계. 외부 소유주 객실(Villa II·III)은 매출 KPI에서 뺀다 */
    @Query(
        """
        select new kr.sunsetcoast.domain.settlement.repository.MonthlyRevenueRow(
            year(l.stayDate), month(l.stayDate), l.room.property.id, l.reservation.source, l.lineType,
            sum(l.grossAmount), sum(l.netAmount), count(l))
        from SettlementLineEntity l
        where l.stayDate between :from and :to
          and l.room.businessOwnerEntity.ownershipType = kr.sunsetcoast.domain.businessowner.vo.OwnershipType.FAMILY
        group by year(l.stayDate), month(l.stayDate), l.room.property.id, l.reservation.source, l.lineType
        """
    )
    fun sumRevenueByMonth(from: LocalDate, to: LocalDate): List<MonthlyRevenueRow>

    @Query("select l from SettlementLineEntity l join fetch l.reservation join fetch l.room where l.importBatchId = :batchId")
    fun findByImportBatchId(batchId: Long): List<SettlementLineEntity>

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

/** 숙박일 월별 매출 집계 행 */
data class MonthlyRevenueRow(
    val year: Int,
    val month: Int,
    val propertyId: Long,
    val source: kr.sunsetcoast.domain.reservation.vo.OtaPlatform,
    val lineType: SettlementLineType,
    val gross: java.math.BigDecimal,
    val net: java.math.BigDecimal,
    val lines: Long,
)
