package jva.cloud.domain.model

/**
 * Domain model representing the input messages used to request suggestions from the AI.
 *
 * @property systemMessage system-level instruction or context for the model.
 * @property userMessage the user's question or prompt to be processed by the model.
 */
data class MessageSuggestion(
    val systemMessage: String,
    val userMessage: String
)
