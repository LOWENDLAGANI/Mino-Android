package com.minetallest.mino.domain

/**
 * Port of `normalizeMemoryText` (client + server guard, spec §10.2).
 * Rejects instruction-shaped text, secrets, duplicates and oversize facts.
 */
object MemoryGuard {

    sealed class Result {
        data class Ok(val text: String) : Result()
        data class Rejected(val reason: String) : Result()
    }

    private val INSTRUCTION_SHAPED = listOf(
        Regex("""^\s*(ignore|forget|disregard|override)\s+(all\s+)?(your\s+|the\s+)?(current\s+|previous\s+|prior\s+|above\s+|earlier\s+)?(your\s+|the\s+)?(instructions?|rules?|prompts?|guidelines?)""", RegexOption.IGNORE_CASE),
        Regex("""^\s*(always|never|only)\s+(answer|respond|reply|write|speak|say|talk|behave|act)\b""", RegexOption.IGNORE_CASE),
        Regex("""^\s*(you|your ai|your assistant)\s+(must|shall|should|will|are going to)\b""", RegexOption.IGNORE_CASE),
        Regex("""^\s*(from now on|from this point on|therefore)\b""", RegexOption.IGNORE_CASE),
        Regex("""^\s*(act as|pretend (to be|you)|roleplay as|behave as)\b""", RegexOption.IGNORE_CASE),
        Regex("""^\s*(answer|respond|reply)\s+(only|always|in)\b(?!\s+(the|a)\s+\?)(?<!i work)""", RegexOption.IGNORE_CASE),
        Regex("""^\s*(no more|stop) (explaining|apologizing|using)\b""", RegexOption.IGNORE_CASE),
    )

    private val SECRETS = listOf(
        Regex("""\bpasswords?\b""", RegexOption.IGNORE_CASE),
        Regex("""\bpasscodes?\b|\bpins?\b""", RegexOption.IGNORE_CASE),
        Regex("""\bapi[-_ ]?keys?\b""", RegexOption.IGNORE_CASE),
        Regex("""\b(client|access|private|secret)[-_ ]?keys?\b""", RegexOption.IGNORE_CASE),
        Regex("""\bsecrets?\b""", RegexOption.IGNORE_CASE),
        Regex("""\btokens?\b""", RegexOption.IGNORE_CASE),
        Regex("""\bcredit card\b|\bcard number\b|\bcvc\b""", RegexOption.IGNORE_CASE),
        Regex("""\bseed phrase\b|\brecovery phrase\b""", RegexOption.IGNORE_CASE),
        Regex("""\b\d{3}[-.\s]?\d{3}[-.\s]?\d{4}\b"""), // phone-shaped
        Regex("""\b(?:\+?\d[\d\s().-]{7,}\d)\b"""), // longer digit runs (account numbers)
        Regex("""\b\d+\s+\w+\s+(street|st|road|rd|avenue|ave|lane|ln|drive|dr|boulevard|blvd)\b""", RegexOption.IGNORE_CASE),
    )

    fun normalize(raw: String, existing: List<Memory> = emptyList()): Result {
        val text = raw.replace(Regex("\\s+"), " ").trim()
        if (text.isEmpty()) return Result.Rejected("Nothing to remember.")
        if (text.length > Refusals.MEMORY_TEXT_LIMIT) {
            return Result.Rejected("Keep it under ${Refusals.MEMORY_TEXT_LIMIT} characters — one short sentence.")
        }
        for (pattern in INSTRUCTION_SHAPED) {
            if (pattern.containsMatchIn(text) || pattern.find(text) != null) {
                return Result.Rejected("Memories are facts, not instructions. Try “I work in TypeScript” instead.")
            }
        }
        for (pattern in SECRETS) {
            if (pattern.containsMatchIn(text)) {
                return Result.Rejected("Don't store passwords, keys or phone numbers here.")
            }
        }
        if (existing.any { it.text.trim().equals(text, ignoreCase = true) }) {
            return Result.Rejected("Mino already remembers that.")
        }
        if (existing.size >= 20) {
            return Result.Rejected("Mino remembers 20 facts already. Remove one to add another.")
        }
        return Result.Ok(text)
    }
}
