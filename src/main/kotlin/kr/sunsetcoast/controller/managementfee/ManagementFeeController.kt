package kr.sunsetcoast.controller.managementfee

import kr.sunsetcoast.controller.managementfee.dto.request.ManagementFeeOverrideRequestBody
import kr.sunsetcoast.controller.managementfee.dto.response.ManagementFeeResponseBody
import kr.sunsetcoast.controller.managementfee.dto.response.MonthlyManagementFeeResponseBody
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
        @RequestParam propertyId: Long,
        @RequestParam @DateTimeFormat(pattern = "yyyy-MM") month: YearMonth,
    ): MonthlyManagementFeeResponseBody =
        MonthlyManagementFeeResponseBody.from(propertyId, month, managementFeeService.findMonthly(propertyId, month))

    @PutMapping("/{id}", version = "1.0")
    override fun override(
        @PathVariable id: Long,
        @RequestBody body: ManagementFeeOverrideRequestBody,
    ): ManagementFeeResponseBody =
        ManagementFeeResponseBody.from(managementFeeService.override(id, body.amount, body.reason))

    @PostMapping("/recalculate", version = "1.0")
    override fun recalculate(): RecalculateResponseBody =
        RecalculateResponseBody(reservations = managementFeeService.recalculateAll())
}
