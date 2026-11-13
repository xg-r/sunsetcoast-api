package kr.sunsetcoast.controller.property.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import kr.sunsetcoast.domain.property.entity.PropertyEntity

@Schema(description = "업소")
data class PropertyResponseBody(
    @field:Schema(description = "업소 ID", example = "1")
    val id: Long,
    @field:Schema(description = "업소 코드", example = "SUNSETCOAST")
    val code: String,
    @field:Schema(description = "업소 이름", example = "선셋코스트 카라반&캠핑장")
    val name: String,
) {
    companion object {
        fun from(entity: PropertyEntity) = PropertyResponseBody(id = entity.id!!, code = entity.code, name = entity.name)
    }
}
