package kr.sunsetcoast.controller.reservation.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import kr.sunsetcoast.domain.reservation.service.DirectReservationService
import kr.sunsetcoast.domain.reservation.vo.PaymentMethod
import kr.sunsetcoast.domain.reservation.vo.SalesChannel
import java.math.BigDecimal
import java.time.LocalDate

@Schema(description = "직접 예약 입력·수정")
data class DirectReservationRequestBody(
    @field:Schema(description = "객실 ID (여러 객실이면 여러 개, 첫 번째가 대표 객실)", example = "[5]")
    val roomIds: List<Long>,
    @field:Schema(description = "입실일", example = "2026-11-20")
    val checkInDate: LocalDate,
    @field:Schema(description = "박수", example = "2")
    val nights: Int,
    @field:Schema(description = "인원", nullable = true, example = "4")
    val guestCount: Int? = null,
    @field:Schema(description = "예약자 이름", example = "홍길동")
    val bookerName: String,
    @field:Schema(description = "예약 채널", allowableValues = ["PHONE", "WALK_IN", "OTHER"], example = "PHONE")
    val salesChannel: SalesChannel,
    @field:Schema(description = "객실·숙박일별 금액 (객실 수 × 박수 만큼)")
    val nightlyPrices: List<NightlyPrice>,
    @field:Schema(description = "할인 (우리 부담, 예약 전체)", example = "20000")
    val discount: BigDecimal = BigDecimal.ZERO,
    @field:Schema(description = "결제수단", nullable = true, allowableValues = ["BANK_TRANSFER", "CARD", "CASH", "OTHER"])
    val paymentMethod: PaymentMethod? = null,
    @field:Schema(description = "입금 완료 여부", example = "false")
    val paid: Boolean = false,
    @field:Schema(description = "메모 (1000자까지)", nullable = true)
    val memo: String? = null,
    @field:Schema(description = "같은 객실·날짜에 예약이 있어도 저장", example = "false")
    val allowOverlap: Boolean = false,
) {
    @Schema(name = "DirectReservationNightlyPrice")
    data class NightlyPrice(
        val roomId: Long,
        @field:Schema(example = "2026-11-20")
        val date: LocalDate,
        @field:Schema(example = "170000")
        val amount: BigDecimal,
    )

    fun toCommand() = DirectReservationService.Command(
        roomIds = roomIds,
        checkInDate = checkInDate,
        nights = nights,
        guestCount = guestCount,
        bookerName = bookerName,
        salesChannel = salesChannel,
        nightlyPrices = nightlyPrices.map { DirectReservationService.RoomNightPrice(it.roomId, it.date, it.amount) },
        discount = discount,
        paymentMethod = paymentMethod,
        paid = paid,
        memo = memo,
        allowOverlap = allowOverlap,
    )
}
