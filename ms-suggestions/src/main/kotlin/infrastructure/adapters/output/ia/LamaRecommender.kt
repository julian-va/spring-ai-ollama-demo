package jva.cloud.infrastructure.adapters.output.ia


import jva.cloud.domain.model.MessageSuggestion
import jva.cloud.domain.port.out.AiRecommender
import jva.cloud.infrastructure.config.OllamaConfig.Companion.OLLAMA_CHAT_CLIENT
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.reactive.asFlow
import org.springframework.ai.chat.messages.AbstractMessage
import org.springframework.ai.chat.messages.SystemMessage
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.ollama.OllamaChatModel
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
class LamaRecommender(
    @param:Qualifier(OLLAMA_CHAT_CLIENT) private val chatModel: OllamaChatModel
) : AiRecommender {
    override fun recommend(messageSuggestion: MessageSuggestion): Flow<String> {
        val builder = StringBuilder()
        val messages: List<AbstractMessage> = listOf(
            SystemMessage(messageSuggestion.systemMessage),
            UserMessage(messageSuggestion.userMessage)
        )

        return flow {
            chatModel.stream(Prompt(messages)).asFlow().collect { chatResponse ->
                chatResponse.result.output.text?.let { text ->
                    builder.append(text)
                }
            }
            emit(builder.toString())
        }
    }
}