package kr.sunsetcoast.domain.settlement.vo

/**
 * 파일에 적힌 객실명을 객실로 찾는다.
 *
 * 1. 별칭과 정확히 일치하면 그 객실
 * 2. 아니면 쉼표로 나눠(다중 객실 예약) 조각마다 찾는다.
 *    - 정확히 일치하는 별칭이 있으면 그 객실
 *    - 잘린 조각("스페이스카라반 쉬멍(오..")은 끝의 "."을 지우고, 그 글자로 시작하는 별칭의 객실이 하나뿐일 때만 그 객실
 * 3. 찾지 못한 조각이 있거나 같은 객실이 두 번 나오면 실패 (관리자가 직접 지정)
 */
object RoomNameMatcher {
    sealed interface Result {
        data class Matched(val roomIds: List<Long>) : Result

        data class Unmatched(val reason: String, val partialRoomIds: List<Long>) : Result
    }

    /** @param aliases 이 플랫폼의 별칭(파일 객실명) → 객실 ID */
    fun match(name: String, aliases: Map<String, Long>): Result {
        val trimmed = name.trim()
        aliases[trimmed]?.let { return Result.Matched(listOf(it)) }

        val parts = trimmed.split(',').map { it.trim() }.filter { it.isNotEmpty() }
        if (parts.size <= 1) return Result.Unmatched("객실 매핑이 없습니다: $trimmed", emptyList())

        val found = parts.map { part -> part to matchPart(part, aliases) }
        val roomIds = found.mapNotNull { it.second }
        val missing = found.filter { it.second == null }.map { it.first }
        return when {
            missing.isNotEmpty() ->
                Result.Unmatched("다중 객실 예약의 객실을 찾지 못했습니다: ${missing.joinToString(", ") { "'$it'" }}", roomIds.distinct())
            roomIds.toSet().size != roomIds.size ->
                Result.Unmatched("다중 객실 예약에 같은 객실이 두 번 나옵니다 (객실명이 잘린 것으로 보임): $trimmed", roomIds.distinct())
            else -> Result.Matched(roomIds)
        }
    }

    private fun matchPart(part: String, aliases: Map<String, Long>): Long? {
        aliases[part]?.let { return it }
        val prefix = part.trimEnd('.', '…', ' ')
        if (prefix.isEmpty()) return null
        return aliases.filterKeys { it.startsWith(prefix) }.values.toSet().singleOrNull()
    }
}
