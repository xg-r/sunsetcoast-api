package kr.sunsetcoast.common.response

data class ErrorResponse(
    val exception: String,
    val message: String,
)
