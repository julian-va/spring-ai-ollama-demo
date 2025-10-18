package jva.cloud.infrastructure.adapters.output.ia


import jva.cloud.domain.model.MessageSuggestion
import jva.cloud.domain.port.out.AiRecommender
import jva.cloud.infrastructure.adapters.entity.GenerationResultEntity
import jva.cloud.infrastructure.adapters.mapper.MessageSuggestionMapper
import jva.cloud.infrastructure.config.OllamaConfig.Companion.OLLAMA_CHAT_CLIENT
import jva.cloud.infrastructure.utils.ResponseUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import org.springframework.ai.chat.messages.AbstractMessage
import org.springframework.ai.chat.messages.SystemMessage
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.ollama.OllamaChatModel
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import kotlin.time.Duration
import kotlin.time.measureTime

/**
 * Adapter that implements the AI recommender using an Ollama chat model.
 *
 * This component is responsible for building the prompt messages, streaming
 * responses when needed, and producing a final formatted generation result.
 *
 * Constructor parameters:
 * @param chatModel the injected Ollama chat model used to produce responses.
 * @param messageSuggestionMapper mapper to convert domain suggestions into entities.
 */
@Component
class LamaRecommender(
    @param:Qualifier(OLLAMA_CHAT_CLIENT) private val chatModel: OllamaChatModel,
    private val messageSuggestionMapper: MessageSuggestionMapper
) : AiRecommender {

    /**
     * Stream raw text responses from the chat model as a Kotlin Flow of strings.
     *
     * The returned flow emits partial text chunks produced by the model. The caller
     * can consume these chunks for incremental UI updates.
     *
     * @param messageSuggestion domain input containing system and user messages.
     * @return a Flow that emits text fragments from the model as they arrive.
     */
    override fun recommendStream(messageSuggestion: MessageSuggestion): Flow<String> {
        val messages: List<AbstractMessage> = createMessages(messageSuggestion = messageSuggestion)

        return chatModel.stream(Prompt(messages)).asFlow().mapNotNull { chatResponse ->
            chatResponse.result.output.text
        }
    }

    /**
     * Produce a single final generation result by consuming the model's stream,
     * concatenating the pieces and formatting the final text for readability.
     *
     * This method measures the duration of the generation operation and returns
     * a {@link GenerationResultEntity} containing the formatted response, elapsed
     * time in milliseconds and the mapped message suggestion entity.
     *
     * @param messageSuggestion domain input containing system and user messages.
     * @return GenerationResultEntity containing the formatted full response and metadata.
     */
    override suspend fun recommend(messageSuggestion: MessageSuggestion): GenerationResultEntity {
        val messages: List<AbstractMessage> = createMessages(messageSuggestion = messageSuggestion)
        var fullResponseRaw = ""
        val duration: Duration = measureTime {
            fullResponseRaw = chatModel
                .stream(Prompt(messages))
                .asFlow().toList().joinToString(separator = "") { chatResponse ->
                    chatResponse.result.output.text.orEmpty()
                }
        }

        // Format the raw response for better readability
        val fullResponse = ResponseUtils.formatFullResponse(fullResponseRaw)

        return GenerationResultEntity(
            fullResponse = fullResponse,
            durationMs = duration.inWholeMilliseconds,
            messageSuggestionEntity = messageSuggestionMapper.toEntity(model = messageSuggestion)
        )
    }

    /**
     * Build the list of messages (system + user) used as prompt for the model.
     *
     * @param messageSuggestion domain object with the system and user messages.
     * @return List of AbstractMessage ready to be supplied to the chat model.
     */
    private fun createMessages(messageSuggestion: MessageSuggestion): List<AbstractMessage> {
        return listOf(
            SystemMessage(messageSuggestion.systemMessage),
            UserMessage(messageSuggestion.userMessage)
        )
    }
}