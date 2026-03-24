package kr.sunsetcoast.domain.settlement.vo

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class SettlementData(
    val fileName: String,
    val settlementFiles: List<SettlementFile>
)

data class SettlementFile(
    val reservationNumber: String,
    val roomName: String,
    val businessNumber: String,
    val reservedAt: LocalDate,
    val checkInDate: LocalDate,
    val nights: Int,
    val bookerName: String? = null,
    val bookerPhone: String? = null,
    val platform: OtaPlatform,
    val inflowChannel: String? = null,
    val reservationStatus: String? = null,
    val settlementStatus: String? = null,
    val grossAmount: BigDecimal,
    val commissionAmount: BigDecimal,
    val netAmount: BigDecimal,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
)
