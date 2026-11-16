package kr.sunsetcoast.domain.settlement.repository

import kr.sunsetcoast.domain.settlement.entity.SettlementLineRevisionEntity
import org.springframework.data.jpa.repository.JpaRepository

interface SettlementLineRevisionRepository : JpaRepository<SettlementLineRevisionEntity, Long> {
    fun findBySettlementLineIdIn(settlementLineIds: Collection<Long>): List<SettlementLineRevisionEntity>

    fun findByImportBatchId(importBatchId: Long): List<SettlementLineRevisionEntity>

    fun findByPreviousImportBatchId(importBatchId: Long): List<SettlementLineRevisionEntity>
}
