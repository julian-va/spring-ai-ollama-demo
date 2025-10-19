package jva.cloud.domain.port.out

import kotlinx.coroutines.flow.Flow
import org.springframework.ai.chat.messages.AbstractMessage
import org.springframework.ai.chat.model.ChatResponse

/**
 * Port defining the AI recommender contract.
 *
 * Implementations provide two forms of recommendations:
 *  - a streaming flow of partial text fragments for incremental consumption
 *  - a synchronous method that returns a full formatted generation result
 */
interface AiRecommenderPort {
    fun sentModelStream(messages: List<AbstractMessage>): Flow<ChatResponse>
}