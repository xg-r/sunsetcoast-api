package kr.sunsetcoast.controller.property.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import kr.sunsetcoast.domain.room.entity.RoomEntity

@Schema(description = "객실")
data class RoomResponseBody(
    @field:Schema(description = "객실 ID", example = "7")
    val id: Long,
    @field:Schema(description = "객실 코드", example = "제돌이")
    val name: String,
    @field:Schema(description = "객실 유형", example = "AIR_STREAM")
    val type: String,
    @field:Schema(description = "업소 ID", example = "1")
    val propertyId: Long,
    @field:Schema(description = "명의 사업자 ID", example = "1")
    val businessOwnerId: Long,
) {
    companion object {
        fun from(entity: RoomEntity) = RoomResponseBody(
            id = entity.id!!,
            name = entity.name,
            type = entity.type,
            propertyId = entity.property.id!!,
            businessOwnerId = entity.businessOwnerEntity.id!!,
        )
    }
}
