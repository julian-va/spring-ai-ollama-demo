package jva.cloud.infrastructure.adapters.entity

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * DTO representing the incoming message payload: a system message and a user message.
 *
 * This class is used for incoming API requests and is mapped from/to JSON properties
 * via Jackson annotations.
 */
data class MessageSuggestionEntity(
    @param:JsonProperty("system_message")
    val systemMessage: String,

    @param:JsonProperty("user_message")
    val userMessage: String
)
