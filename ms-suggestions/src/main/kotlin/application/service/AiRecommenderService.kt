package application.service

import application.port.output.AiRecommenderPort
import application.usecase.AiRecommenderUseCase
import domain.model.GenerationResult
import domain.model.MessageSuggestion
import infrastructure.utils.ResponseUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.messages.AbstractMessage
import org.springframework.ai.chat.messages.SystemMessage
import org.springframework.ai.chat.messages.UserMessage
import kotlin.time.Duration
import kotlin.time.measureTime

/**
 * Service implementation of the AiRecommender use case.
 *
 * This class adapts the domain use case to an implementation that uses an
 * external AI model port to obtain streaming and synchronous recommendations.
 *
 * @property aiRecommenderPort implementation of the outbound port used to call
 * the underlying chat model.
 */
class AiRecommenderService(private val aiRecommenderPort: AiRecommenderPort) : AiRecommenderUseCase {
    private val logger: Logger = LoggerFactory.getLogger(AiRecommenderService::class.java)

    override fun recommendStream(messageSuggestion: MessageSuggestion): Flow<String> {
        val messages: List<AbstractMessage> = createMessages(messageSuggestion = messageSuggestion)

        return aiRecommenderPort.sentModelStream(messages = messages).onEach {
            logger.info("Received streaming fragment: {}", it.result)
        }.mapNotNull { chatResponse ->
            logger.debug("Received streaming fragment: {}", chatResponse.result)
            chatResponse.result.output.text
        }
    }

    override suspend fun recommend(messageSuggestion: MessageSuggestion): GenerationResult {
        val messages: List<AbstractMessage> = createMessages(messageSuggestion = messageSuggestion)
        var fullResponseRaw = ""
        val duration: Duration = measureTime {
            fullResponseRaw = aiRecommenderPort.sentModelStream(messages = messages).toList()
                .joinToString(separator = "") { chatResponse ->
                    chatResponse.result.output.text.orEmpty()
                }
        }

        return GenerationResult(
            fullResponse = ResponseUtils.formatFullResponse(fullResponseRaw),
            durationMs = duration.inWholeMilliseconds,
            messageSuggestion = messageSuggestion
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