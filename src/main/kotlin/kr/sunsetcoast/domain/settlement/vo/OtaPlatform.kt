package kr.sunsetcoast.domain.settlement.vo

enum class OtaPlatform {
    DDNAYO, ;

    companion object {
        fun from(value: String): OtaPlatform {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
                ?: throw IllegalArgumentException("Unsupported OTA platform: $value")
        }
    }
}
