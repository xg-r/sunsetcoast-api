package kr.sunsetcoast.controller.property

import kr.sunsetcoast.controller.property.dto.response.PropertyResponseBody
import kr.sunsetcoast.domain.property.service.PropertyService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/properties")
class PropertyController(
    private val propertyService: PropertyService,
) : PropertyControllerDocs {
    @GetMapping(version = "1.0")
    override fun getProperties(): List<PropertyResponseBody> =
        propertyService.findAll().map { PropertyResponseBody.from(it) }
}
