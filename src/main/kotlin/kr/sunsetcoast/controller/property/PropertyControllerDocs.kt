package kr.sunsetcoast.controller.property

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import kr.sunsetcoast.controller.property.dto.response.PropertyResponseBody
import kr.sunsetcoast.controller.property.dto.response.RoomResponseBody

@Tag(name = "Property", description = "업소")
interface PropertyControllerDocs {
    @Operation(summary = "업소 목록")
    fun getProperties(): List<PropertyResponseBody>

    @Operation(summary = "업소의 객실 목록")
    fun getRooms(@Parameter(description = "업소 ID", example = "1") propertyId: Long): List<RoomResponseBody>
}
