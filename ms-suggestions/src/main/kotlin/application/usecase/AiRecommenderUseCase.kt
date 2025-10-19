package jva.cloud.application.usecase

import jva.cloud.domain.model.GenerationResult
import jva.cloud.domain.model.MessageSuggestion
import kotlinx.coroutines.flow.Flow

/**
 * Use case port that defines AI recommendation operations.
 *
 * Implementations provide streaming and blocking recommendation methods
 * using a domain-level MessageSuggestion as input.
 */
interface AiRecommenderUseCase {
    /**
     * Stream partial textual responses produced by the model.
     *
     * @param messageSuggestion domain object containing system and user messages.
     * @return a Flow emitting partial text fragments from the chat model.
     */
    fun recommendStream(messageSuggestion: MessageSuggestion): Flow<String>

    /**
     * Execute the recommendation and return a fully-formed GenerationResult.
     *
     * @param messageSuggestion domain object containing system and user messages.
     * @return a GenerationResult with formatted full response and timing metadata.
     */
    suspend fun recommend(messageSuggestion: MessageSuggestion): GenerationResult
}