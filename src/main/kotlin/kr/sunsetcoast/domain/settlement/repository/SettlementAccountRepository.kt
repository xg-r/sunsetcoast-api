package kr.sunsetcoast.domain.settlement.repository

import kr.sunsetcoast.domain.reservation.vo.OtaPlatform
import kr.sunsetcoast.domain.settlement.entity.SettlementAccountEntity
import org.springframework.data.jpa.repository.JpaRepository

interface SettlementAccountRepository : JpaRepository<SettlementAccountEntity, Long> {
    fun existsBySourceAndAccount(source: OtaPlatform, account: String): Boolean
}
