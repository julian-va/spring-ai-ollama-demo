package jva.cloud.domain.port.out

import jva.cloud.domain.model.MessageSuggestion
import jva.cloud.infrastructure.adapters.entity.GenerationResultEntity
import kotlinx.coroutines.flow.Flow

/**
 * Port defining the AI recommender contract.
 *
 * Implementations provide two forms of recommendations:
 *  - a streaming flow of partial text fragments for incremental consumption
 *  - a synchronous method that returns a full formatted generation result
 */
interface AiRecommender {
    /**
     * Stream partial text fragments produced by the model.
     *
     * @param messageSuggestion domain input containing system and user messages.
     * @return a Flow emitting text fragments as they arrive.
     */
    fun recommendStream(messageSuggestion: MessageSuggestion): Flow<String>

    /**
     * Produce a full generation result by consuming the model's stream and
     * returning the final formatted output together with metadata.
     *
     * @param messageSuggestion domain input containing system and user messages.
     * @return a GenerationResultEntity containing the formatted full response and metadata.
     */
    suspend fun recommend(messageSuggestion: MessageSuggestion): GenerationResultEntity
}