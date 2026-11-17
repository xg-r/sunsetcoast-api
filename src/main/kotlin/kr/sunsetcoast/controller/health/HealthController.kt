package kr.sunsetcoast.controller.health

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Health", description = "서버 상태 (로그인 없이)")
interface HealthControllerDocs {
    @Operation(summary = "서버 상태", description = "서버가 떠 있으면 {\"status\":\"UP\"}. 배포 확인·모니터링용")
    fun health(): Map<String, String>
}

// 사용자용 공개 API(/public 아래)의 첫 경로. api.sunsetcoast.kr에서 밖으로 열리는 경로다
@RestController
class HealthController : HealthControllerDocs {
    @GetMapping("/public/health", version = "1.0")
    override fun health(): Map<String, String> = mapOf("status" to "UP")
}
