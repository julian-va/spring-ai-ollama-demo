package infrastructure.adapters.dto

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Persistence / transport representation of a generation result.
 *
 * This DTO is used for API responses and persistence and maps JSON properties
 * to the Kotlin data class fields.
 */
data class GenerationResultDto(
    @param:JsonProperty("full_response")
    val fullResponse: String,
    @param:JsonProperty("duration_ms")
    val durationMs: Long,
    @param:JsonProperty("message_suggestion")
    val requestMessageSuggestionDto: RequestMessageSuggestionDto
)
