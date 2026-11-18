package kr.sunsetcoast.controller.managementfee

import kr.sunsetcoast.config.security.CurrentUser
import kr.sunsetcoast.controller.managementfee.dto.request.ManagementFeeExtrasRequestBody
import kr.sunsetcoast.controller.managementfee.dto.request.ManagementFeeOverrideRequestBody
import kr.sunsetcoast.controller.managementfee.dto.request.ManagementFeePaymentRequestBody
import kr.sunsetcoast.controller.managementfee.dto.request.ManagementFeeVerificationRequestBody
import kr.sunsetcoast.controller.managementfee.dto.response.ManagementFeeResponseBody
import kr.sunsetcoast.controller.managementfee.dto.response.MonthlyManagementFeeResponseBody
import kr.sunsetcoast.controller.managementfee.dto.response.PaidManagementFeeSummaryResponseBody
import kr.sunsetcoast.controller.staycalendar.dto.response.RecalculateResponseBody
import kr.sunsetcoast.domain.managementfee.service.ManagementFeeService
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.YearMonth

@RestController
@RequestMapping("/management-fees")
class ManagementFeeController(
    private val managementFeeService: ManagementFeeService,
) : ManagementFeeControllerDocs {
    @GetMapping(version = "1.0")
    override fun getMonthly(
        @RequestParam(required = false) propertyId: Long?,
        @RequestParam(required = false) businessOwnerId: Long?,
        @RequestParam @DateTimeFormat(pattern = "yyyy-MM") month: YearMonth,
    ): MonthlyManagementFeeResponseBody {
        val fees = managementFeeService.findMonthly(propertyId, businessOwnerId, month)
        return MonthlyManagementFeeResponseBody.from(propertyId, businessOwnerId, month, fees, managementFeeService.revenueOf(fees))
    }

    @PutMapping("/{id}/extras", version = "1.0")
    override fun replaceExtras(
        @PathVariable id: Long,
        @RequestBody body: ManagementFeeExtrasRequestBody,
    ): ManagementFeeResponseBody {
        val fee = managementFeeService.replaceExtras(id, body.extras.map { it.toInput() })
        return ManagementFeeResponseBody.from(fee, managementFeeService.revenueOf(listOf(fee))[fee.id])
    }

    @PutMapping("/{id}", version = "1.0")
    override fun override(
        @PathVariable id: Long,
        @RequestBody body: ManagementFeeOverrideRequestBody,
    ): ManagementFeeResponseBody =
        ManagementFeeResponseBody.from(managementFeeService.override(id, body.amount, body.reason))

    @PostMapping("/verifications", version = "1.0")
    override fun markVerified(@RequestBody body: ManagementFeeVerificationRequestBody): List<ManagementFeeResponseBody> =
        managementFeeService.markVerified(body.ids, body.verified, CurrentUser.get()?.userId)
            .map { ManagementFeeResponseBody.from(it) }

    @GetMapping("/paid-summary", version = "1.0")
    override fun getPaidSummary(
        @RequestParam @DateTimeFormat(pattern = "yyyy-MM") from: YearMonth,
        @RequestParam @DateTimeFormat(pattern = "yyyy-MM") to: YearMonth,
        @RequestParam(defaultValue = "FEE_MONTH") basis: String,
    ): PaidManagementFeeSummaryResponseBody {
        val paidBasis = runCatching { ManagementFeeService.PaidBasis.valueOf(basis) }.getOrNull()
            ?: throw IllegalArgumentException("알 수 없는 기준입니다: $basis")
        return PaidManagementFeeSummaryResponseBody.from(managementFeeService.paidSummary(from, to, paidBasis))
    }

    @PostMapping("/payments", version = "1.0")
    override fun markPayments(@RequestBody body: ManagementFeePaymentRequestBody): List<ManagementFeeResponseBody> =
        managementFeeService.markPayments(body.ids, body.paid, CurrentUser.get()?.userId)
            .map { ManagementFeeResponseBody.from(it) }

    @PostMapping("/recalculate", version = "1.0")
    override fun recalculate(): RecalculateResponseBody =
        RecalculateResponseBody(reservations = managementFeeService.recalculateAll())
}
