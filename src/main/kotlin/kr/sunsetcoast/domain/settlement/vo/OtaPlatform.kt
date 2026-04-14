package kr.sunsetcoast.domain.settlement.vo

import kr.sunsetcoast.domain.settlement.vo.OtaPlatform.entries

enum class OtaPlatform {
    DDNAYO, YEOGI, NOL,
    ;

    companion object {
        fun from(value: String): OtaPlatform {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
                ?: throw IllegalArgumentException("Unsupported OTA platform: $value")
        }
    }
}
