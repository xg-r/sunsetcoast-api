package kr.sunsetcoast.controller.businessowner.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import kr.sunsetcoast.controller.property.dto.response.RoomResponseBody
import kr.sunsetcoast.domain.businessowner.service.BusinessOwnerService

@Schema(description = "사업자와 명의 객실")
data class BusinessOwnerResponseBody(
    @field:Schema(description = "사업자 ID", example = "1")
    val id: Long,
    @field:Schema(description = "사업자명", example = "선셋코스트")
    val name: String,
    @field:Schema(description = "FAMILY: 가족 사업자, EXTERNAL: 외부 소유주 (Villa II·III)", allowableValues = ["FAMILY", "EXTERNAL"])
    val ownershipType: String,
    @field:Schema(description = "명의 객실 (여러 업소에 걸칠 수 있다)")
    val rooms: List<RoomResponseBody>,
) {
    companion object {
        fun from(value: BusinessOwnerService.OwnerRooms) = BusinessOwnerResponseBody(
            id = value.owner.id!!,
            name = value.owner.name,
            ownershipType = value.owner.ownershipType.name,
            rooms = value.rooms.map { RoomResponseBody.from(it) },
        )
    }
}
