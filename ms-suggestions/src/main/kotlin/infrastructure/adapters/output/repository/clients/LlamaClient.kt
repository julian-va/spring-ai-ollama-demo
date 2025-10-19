package jva.cloud.infrastructure.adapters.output.repository.clients

import jva.cloud.domain.port.out.AiRecommenderPort
import jva.cloud.infrastructure.config.OllamaConfig.Companion.OLLAMA_CHAT_CLIENT
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import org.springframework.ai.chat.messages.AbstractMessage
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.ollama.OllamaChatModel
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Repository

/**
 * Repository client that adapts the Ollama chat model to the domain outbound port.
 *
 * This class delegates streaming chat requests to an injected OllamaChatModel
 * and exposes the results as a Kotlin Flow of ChatResponse.
 *
 * @property chatModel the configured Ollama chat model bean
 */
@Repository
class LlamaClient(@param:Qualifier(OLLAMA_CHAT_CLIENT) private val chatModel: OllamaChatModel) : AiRecommenderPort {
    /**
     * Stream chat responses from the Ollama model as a Flow.
     *
     * @param messages list of AbstractMessage that form the prompt
     * @return Flow emitting ChatResponse items from the model stream
     */
    override fun sentModelStream(messages: List<AbstractMessage>): Flow<ChatResponse> {
        return chatModel.stream(Prompt(messages)).asFlow()
    }
}