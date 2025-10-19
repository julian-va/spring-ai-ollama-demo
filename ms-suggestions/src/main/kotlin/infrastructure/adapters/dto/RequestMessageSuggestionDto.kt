package infrastructure.adapters.dto

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank

/**
 * DTO representing the incoming message payload: a system message and a user message.
 *
 * This class is used for incoming API requests and is mapped from/to JSON properties
 * via Jackson annotations.
 */
data class RequestMessageSuggestionDto(
    @param:JsonProperty("system_message")
    @field:NotBlank
    val systemMessage: String?,

    @param:JsonProperty("user_message")
    @field:NotBlank
    val userMessage: String?
)
