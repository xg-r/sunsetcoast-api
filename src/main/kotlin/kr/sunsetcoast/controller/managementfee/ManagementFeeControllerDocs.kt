package kr.sunsetcoast.controller.managementfee

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import kr.sunsetcoast.controller.managementfee.dto.request.ManagementFeeOverrideRequestBody
import kr.sunsetcoast.controller.managementfee.dto.response.ManagementFeeResponseBody
import kr.sunsetcoast.controller.managementfee.dto.response.MonthlyManagementFeeResponseBody
import kr.sunsetcoast.controller.staycalendar.dto.response.RecalculateResponseBody
import java.time.YearMonth

@Tag(name = "Management Fee", description = "현장 관리비 (SETTLE-02)")
interface ManagementFeeControllerDocs {
    @Operation(
        summary = "월별 관리비 조회",
        description = "입실일이 그 달에 속한 예약의 관리비. 예약번호의 객실마다 1회(박수 무관), 취소는 0원. " +
            "업소나 사업자로 좁힐 수 있다. 사업자로 보면 그 사업자 명의 객실만 업소와 상관없이 모은다 (사업자별 지급).",
    )
    fun getMonthly(
        @Parameter(description = "업소 ID (생략하면 전체)", example = "1") propertyId: Long?,
        @Parameter(description = "사업자 ID (생략하면 전체)", example = "1") businessOwnerId: Long?,
        @Parameter(description = "입실 월 (yyyy-MM)", example = "2026-08") month: YearMonth,
    ): MonthlyManagementFeeResponseBody

    @Operation(
        summary = "관리비 수정",
        description = "한달살기 등 예외 건의 금액을 고친다. 자동 계산 금액은 그대로 남고, 이후 업로드·재계산이 고친 금액을 덮어쓰지 않는다. " +
            "amount를 null로 보내면 수정을 취소한다.",
    )
    fun override(
        @Parameter(description = "관리비 ID", example = "12") id: Long,
        body: ManagementFeeOverrideRequestBody,
    ): ManagementFeeResponseBody

    @Operation(
        summary = "관리비 전체 재계산",
        description = "단가를 고친 뒤 실행한다. 관리자가 고친 금액은 유지된다.",
    )
    fun recalculate(): RecalculateResponseBody
}
