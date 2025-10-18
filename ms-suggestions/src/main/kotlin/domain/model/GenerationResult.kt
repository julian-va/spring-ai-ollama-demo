package jva.cloud.domain.model

/**
 * Domain model representing the result of a text generation operation.
 *
 * @property fullResponse the formatted/full textual response produced by the model.
 * @property durationMs elapsed time in milliseconds that the generation took.
 * @property messageSuggestion the original message suggestion that produced this result.
 */
data class GenerationResult(
    val fullResponse: String,
    val durationMs: Long,
    val messageSuggestion: MessageSuggestion
)
