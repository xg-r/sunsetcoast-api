package kr.sunsetcoast.controller.reservation.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import kr.sunsetcoast.domain.reservation.entity.ReservationEntity
import kr.sunsetcoast.domain.reservation.vo.ReservationSummary
import kr.sunsetcoast.domain.room.entity.RoomEntity
import java.math.BigDecimal
import java.time.LocalDate

@Schema(description = "예약")
data class ReservationResponseBody(
    @field:Schema(description = "예약 ID", example = "656")
    val id: Long,
    @field:Schema(description = "정산 플랫폼 (DIRECT: 관리자가 직접 입력)", allowableValues = ["DDNAYO", "YEOGI", "NOL", "DIRECT"], example = "YEOGI")
    val source: String,
    @field:Schema(description = "플랫폼 예약번호", example = "26061517003670YE1")
    val reservationNumber: String,
    @field:Schema(description = "고객이 예약한 채널", allowableValues = ["NAVER", "YANOLJA", "YEOGI", "DDNAYO", "HOMEPAGE", "PHONE", "WALK_IN", "OTHER"])
    val salesChannel: String,
    @field:Schema(description = "파일에 적힌 채널 원문", nullable = true, example = "네이버")
    val salesChannelRaw: String?,
    @field:Schema(description = "대표 객실 ID (다중 객실 예약은 첫 번째 객실)")
    val roomId: Long,
    @field:Schema(description = "대표 객실 코드", example = "RESORT_305")
    val roomName: String,
    @field:Schema(description = "예약의 모든 객실 (다중 객실 예약은 2개 이상)")
    val rooms: List<RoomRef>,
    @field:Schema(description = "예약자 (플랫폼에 따라 마스킹)", nullable = true, example = "이*얀")
    val bookerName: String?,
    @field:Schema(description = "예약 일시", nullable = true, example = "2026-07-15T17:00:36")
    val reservedAt: String?,
    @field:Schema(description = "입실일. 취소수수료만 있는 예약은 null", nullable = true, example = "2026-08-03")
    val checkInDate: String?,
    @field:Schema(description = "목록 기준일: 입실일, 없으면 매출인식일", nullable = true, example = "2026-08-03")
    val baseDate: String?,
    @field:Schema(description = "박수", nullable = true, example = "2")
    val nights: Int?,
    @field:Schema(description = "주말 밤 포함 여부. 숙박일을 모르면 null", nullable = true)
    val weekend: Boolean?,
    @field:Schema(description = "성수기 밤 포함 여부. 판단 근거가 없으면 null", nullable = true)
    val peakSeason: Boolean?,
    @field:Schema(description = "CONFIRMED: 투숙, CANCELLED: 취소", allowableValues = ["CONFIRMED", "CANCELLED"])
    val status: String,
    @field:Schema(description = "매출 합계", example = "250000")
    val grossAmount: BigDecimal,
    @field:Schema(description = "수수료 합계", example = "37500")
    val commissionAmount: BigDecimal,
    @field:Schema(description = "정산금액(입금액) 합계", example = "212500")
    val netAmount: BigDecimal,
    @field:Schema(description = "현장 관리비 (객실 합계, 수정 금액 반영). 계산 전이거나 슈퍼어드민이 아니면 null", nullable = true, example = "30000")
    val managementFee: BigDecimal?,
    @field:Schema(description = "US: 우리 입금, OWNER: 외부 소유주 계좌로 직접 입금 (매출 제외)", allowableValues = ["US", "OWNER"])
    val paymentReceiver: String,
    @field:Schema(description = "인원 (직접 예약)", nullable = true)
    val guestCount: Int?,
    @field:Schema(description = "결제수단 (직접 예약)", nullable = true, allowableValues = ["BANK_TRANSFER", "CARD", "CASH", "OTHER"])
    val paymentMethod: String?,
    @field:Schema(description = "입금 여부 (직접 예약)", nullable = true)
    val paid: Boolean?,
    @field:Schema(description = "메모", nullable = true)
    val memo: String?,
) {
    @Schema(name = "ReservationRoom", description = "예약 객실")
    data class RoomRef(val id: Long, val name: String)

    companion object {
        /** @param includeManagementFee 관리비는 슈퍼어드민만 본다 */
        fun from(summary: ReservationSummary, includeManagementFee: Boolean) =
            of(summary.reservation, summary.rooms, summary.baseDate, summary.managementFee.takeIf { includeManagementFee })

        fun of(r: ReservationEntity, rooms: List<RoomEntity>, baseDate: LocalDate?, managementFee: BigDecimal?) = ReservationResponseBody(
            id = r.id!!,
            source = r.source.name,
            reservationNumber = r.reservationNumber,
            salesChannel = r.salesChannel.name,
            salesChannelRaw = r.salesChannelRaw,
            roomId = r.room.id!!,
            roomName = r.room.name,
            rooms = rooms.map { RoomRef(it.id!!, it.name) },
            bookerName = r.bookerName,
            reservedAt = r.reservedAt?.withNano(0)?.toString(),
            checkInDate = r.checkInDate?.toString(),
            baseDate = baseDate?.toString(),
            nights = r.nights,
            weekend = r.weekend,
            peakSeason = r.peakSeason,
            status = r.status.name,
            grossAmount = r.grossAmount,
            commissionAmount = r.commissionAmount,
            netAmount = r.netAmount,
            managementFee = managementFee,
            paymentReceiver = r.paymentReceiver.name,
            guestCount = r.guestCount,
            paymentMethod = r.paymentMethod?.name,
            paid = r.paid,
            memo = r.memo,
        )
    }
}
