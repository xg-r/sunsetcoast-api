package kr.sunsetcoast.controller.businessowner

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import kr.sunsetcoast.controller.businessowner.dto.response.BusinessOwnerResponseBody

@Tag(name = "Business Owner", description = "사업자")
interface BusinessOwnerControllerDocs {
    @Operation(summary = "사업자 목록", description = "사업자별 명의 객실 포함. 연락처·사업자번호 등은 내려주지 않는다.")
    fun getBusinessOwners(): List<BusinessOwnerResponseBody>
}
