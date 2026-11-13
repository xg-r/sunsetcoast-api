package kr.sunsetcoast.controller.property

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import kr.sunsetcoast.controller.property.dto.response.PropertyResponseBody

@Tag(name = "Property", description = "업소")
interface PropertyControllerDocs {
    @Operation(summary = "업소 목록")
    fun getProperties(): List<PropertyResponseBody>
}
