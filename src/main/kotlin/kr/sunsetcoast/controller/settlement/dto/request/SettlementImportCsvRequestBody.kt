package kr.sunsetcoast.controller.settlement.dto.request

import com.fasterxml.jackson.annotation.JsonProperty

data class SettlementImportCsvRequestBody(
    @get:JsonProperty("otaPlatform") val otaPlatform: String,
)
