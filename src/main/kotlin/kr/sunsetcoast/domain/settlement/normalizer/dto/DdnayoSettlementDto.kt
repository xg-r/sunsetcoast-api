package kr.sunsetcoast.domain.settlement.normalizer.dto

import kr.sunsetcoast.domain.settlement.reader.ddnayo.DdnayoHeaders
import kr.sunsetcoast.domain.settlement.vo.OtaPlatform
import kr.sunsetcoast.domain.settlement.vo.SettlementFile
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class DdnayoSettlementDto(
    val reservationNumber: String,
    val roomName: String,
    val reservedAt: LocalDate,
    val inflow: String,
    val bookerName: String,
    val phoneNumber: String,
    val checkInDate: LocalDate,
    val nights: Int,
    val grossAmount: Long,
    val settlementAmount: Long,
    val commissionAmount: Long,
    val settlementStatus: String,
    val paymentType: String,
    val reservationStatus: String,
    val businessNumber: String,
) {
    fun toSettlementFile(): SettlementFile {
        return SettlementFile(
            reservationNumber = reservationNumber,
            roomName = roomName,
            businessNumber = businessNumber,
            reservedAt = reservedAt,
            checkInDate = checkInDate,
            nights = nights,
            bookerName = bookerName,
            bookerPhone = phoneNumber,
            platform = OtaPlatform.DDNAYO,
            inflowChannel = inflow,
            reservationStatus = reservationStatus,
            settlementStatus = settlementStatus,
            grossAmount = BigDecimal.valueOf(grossAmount),
            commissionAmount = BigDecimal.valueOf(commissionAmount),
            netAmount = BigDecimal.valueOf(settlementAmount),
        )
    }

    companion object {
        private val ddnayoLocalDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        fun from(row: Map<String, String>): DdnayoSettlementDto {
            return DdnayoSettlementDto(
                reservationNumber = row[DdnayoHeaders.RESERVATION_NUMBER].orEmpty(),
                roomName = row[DdnayoHeaders.ROOM_NAME].orEmpty(),
                reservedAt = LocalDate.parse(
                    row[DdnayoHeaders.RESERVATION_DATE].orEmpty(),
                    ddnayoLocalDateFormatter,
                ),
                inflow = row[DdnayoHeaders.INFLOW].orEmpty(),
                bookerName = row[DdnayoHeaders.RESERVER_NAME].orEmpty(),
                phoneNumber = row[DdnayoHeaders.PHONE].orEmpty(),
                checkInDate = LocalDate.parse(
                    row[DdnayoHeaders.USE_DATE].orEmpty(),
                    ddnayoLocalDateFormatter,
                ),
                nights = row[DdnayoHeaders.NIGHTS]?.toIntOrNull() ?: 0,
                grossAmount = parseAmount(row[DdnayoHeaders.SALES]),
                settlementAmount = parseAmount(row[DdnayoHeaders.SETTLEMENT]),
                commissionAmount = parseAmount(row[DdnayoHeaders.COMMISSION]),
                settlementStatus = row[DdnayoHeaders.SETTLEMENT_STATUS].orEmpty(),
                paymentType = row[DdnayoHeaders.PAYMENT_TYPE].orEmpty(),
                reservationStatus = row[DdnayoHeaders.RESERVATION_STATUS].orEmpty(),
                businessNumber = row[DdnayoHeaders.BUSINESS_NUMBER].orEmpty(),
            )
        }

        private fun parseAmount(value: String?): Long {
            return value
                ?.replace(",", "")
                ?.trim()
                ?.toLongOrNull()
                ?: 0L
        }
    }
}
