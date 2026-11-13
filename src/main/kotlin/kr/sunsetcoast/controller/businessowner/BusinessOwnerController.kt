package kr.sunsetcoast.controller.businessowner

import kr.sunsetcoast.controller.businessowner.dto.response.BusinessOwnerResponseBody
import kr.sunsetcoast.domain.businessowner.service.BusinessOwnerService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/business-owners")
class BusinessOwnerController(
    private val businessOwnerService: BusinessOwnerService,
) : BusinessOwnerControllerDocs {
    @GetMapping(version = "1.0")
    override fun getBusinessOwners(): List<BusinessOwnerResponseBody> =
        businessOwnerService.findAllWithRooms().map { BusinessOwnerResponseBody.from(it) }
}
