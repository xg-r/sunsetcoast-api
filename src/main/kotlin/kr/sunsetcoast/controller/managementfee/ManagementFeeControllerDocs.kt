package kr.sunsetcoast.controller.managementfee

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import kr.sunsetcoast.controller.managementfee.dto.request.ManagementFeeExtrasRequestBody
import kr.sunsetcoast.controller.managementfee.dto.request.ManagementFeeOverrideRequestBody
import kr.sunsetcoast.controller.managementfee.dto.request.ManagementFeePaymentRequestBody
import kr.sunsetcoast.controller.managementfee.dto.request.ManagementFeeVerificationRequestBody
import kr.sunsetcoast.controller.managementfee.dto.response.ManagementFeeResponseBody
import kr.sunsetcoast.controller.managementfee.dto.response.MonthlyManagementFeeResponseBody
import kr.sunsetcoast.controller.managementfee.dto.response.PaidManagementFeeSummaryResponseBody
import kr.sunsetcoast.controller.staycalendar.dto.response.RecalculateResponseBody
import java.time.YearMonth

@Tag(name = "Management Fee", description = "현장 관리비 (SETTLE-02)")
interface ManagementFeeControllerDocs {
    @Operation(
        summary = "월별 관리비 조회",
        description = "입실일이 그 달에 속한 예약의 관리비. 예약번호의 객실마다 1회(박수 무관), 취소는 0원. " +
            "행마다 추가요금과 그 객실의 매출(판매가·정산금액)을 같이 준다. " +
            "업소나 사업자로 좁힐 수 있다. 사업자로 보면 그 사업자 명의 객실만 업소와 상관없이 모은다 (사업자별 지급).",
    )
    fun getMonthly(
        @Parameter(description = "업소 ID (생략하면 전체)", example = "1") propertyId: Long?,
        @Parameter(description = "사업자 ID (생략하면 전체)", example = "1") businessOwnerId: Long?,
        @Parameter(description = "입실 월 (yyyy-MM)", example = "2026-08") month: YearMonth,
    ): MonthlyManagementFeeResponseBody

    @Operation(
        summary = "관리비 추가요금 저장",
        description = "손님이 현장에 낼 추가요금(바베큐, 추가인원 등)을 숙박비와 함께 우리 계좌로 보낸 경우, 관리비와 함께 현장에 이체할 금액으로 적는다. " +
            "보낸 목록으로 통째로 바꾸고 빈 목록이면 모두 지운다. 금액 = 단가 × 수량 × 박수(추가인원만, 생략하면 예약 박수). " +
            "지급액(payableAmount) = 관리비 + 추가요금이 되어, 확인한 뒤 바꾸면 '다시 확인'이 된다. 입금 완료된 관리비는 바꿀 수 없다.",
    )
    fun replaceExtras(
        @Parameter(description = "관리비 ID", example = "12") id: Long,
        body: ManagementFeeExtrasRequestBody,
    ): ManagementFeeResponseBody

    @Operation(
        summary = "관리비 수정",
        description = "한달살기 등 예외 건의 금액을 고친다. 자동 계산 금액은 그대로 남고, 이후 업로드·재계산이 고친 금액을 덮어쓰지 않는다. " +
            "amount를 null로 보내면 수정을 취소한다. 입금 완료된 관리비는 고칠 수 없다 (입금 완료를 먼저 취소).",
    )
    fun override(
        @Parameter(description = "관리비 ID", example = "12") id: Long,
        body: ManagementFeeOverrideRequestBody,
    ): ManagementFeeResponseBody

    @Operation(
        summary = "관리비 확인 표시",
        description = "관리자가 실제 자료와 맞춰 보고 맞다고 표시한다 (입금 완료와 별개). 확인할 때의 지급액을 남겨, " +
            "그 뒤 업로드·재계산·수정으로 금액이 바뀌면 verificationStatus=CHANGED(다시 확인 필요)가 된다. verified=false면 취소.",
    )
    fun markVerified(body: ManagementFeeVerificationRequestBody): List<ManagementFeeResponseBody>

    @Operation(
        summary = "월별 관리비 입금 내역",
        description = "입금 완료된 관리비만, 객실 명의 사업자 × 월로 모은다 (금액은 입금 완료로 표시할 때의 지급액 = 관리비 + 추가요금). " +
            "basis=FEE_MONTH면 입실 월, PAID_MONTH면 입금 완료로 표시한 달 기준. 최대 36개월.",
    )
    fun getPaidSummary(
        @Parameter(description = "시작 월", example = "2026-08") from: YearMonth,
        @Parameter(description = "끝 월", example = "2026-10") to: YearMonth,
        @Parameter(description = "FEE_MONTH(입실 월) 또는 PAID_MONTH(입금 표시한 달)", example = "FEE_MONTH") basis: String,
    ): PaidManagementFeeSummaryResponseBody

    @Operation(
        summary = "관리비 입금 확인",
        description = "관리자가 현장에 입금한 뒤 입금 완료로 표시한다 (paid=false면 취소). 여러 건을 한꺼번에 처리한다. " +
            "정산 파일 업로드·재계산은 입금 상태를 바꾸지 않는다. 지급액이 0원인 행은 표시할 수 없다. " +
            "표시할 때의 지급액(paidAmount)을 남겨 두어 그 뒤 금액이 바뀌면 paymentStatus=PAID_CHANGED로 알려 준다.",
    )
    fun markPayments(body: ManagementFeePaymentRequestBody): List<ManagementFeeResponseBody>

    @Operation(
        summary = "관리비 전체 재계산",
        description = "단가를 고친 뒤 실행한다. 관리자가 고친 금액은 유지된다.",
    )
    fun recalculate(): RecalculateResponseBody
}
