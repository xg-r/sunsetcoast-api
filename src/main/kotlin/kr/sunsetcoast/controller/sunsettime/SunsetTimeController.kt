package kr.sunsetcoast.controller.sunsettime

import kr.sunsetcoast.controller.sunsettime.dto.response.SunsetTimeResponseBody
import kr.sunsetcoast.domain.sunsettime.service.SunsetTimeService
import kr.sunsetcoast.domain.sunsettime.vo.SunsetTime
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class SunsetTimeController(
    private val sunsetTimeService: SunsetTimeService,
) : SunsetTimeControllerDocs {
    @GetMapping("/sunset-time")
    override fun getSunsetTime(): ResponseEntity<SunsetTimeResponseBody> {
        val sunsetTime: SunsetTime =sunsetTimeService.getSunsetTime()
        return ResponseEntity.ok(SunsetTimeResponseBody.from(sunsetTime))
    }
}
