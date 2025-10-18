package infrastructure.adapters.output.ia

import jva.cloud.domain.model.MessageSuggestion
import jva.cloud.infrastructure.adapters.entity.MessageSuggestionEntity
import jva.cloud.infrastructure.adapters.mapper.MessageSuggestionMapper
import jva.cloud.infrastructure.adapters.output.ia.LamaRecommender
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.ollama.OllamaChatModel
import reactor.core.publisher.Flux
import kotlin.test.assertEquals

@ExtendWith(MockitoExtension::class)
class LamaRecommenderTest {

    @Mock
    lateinit var chatModel: OllamaChatModel

    @Mock
    lateinit var messageSuggestionMapper: MessageSuggestionMapper

    @InjectMocks
    lateinit var recommender: LamaRecommender

    @Test
    fun `should return concatenated text from chat responses`() = runTest {
        // Given
        val messageSuggestion = MessageSuggestion(
            systemMessage = "Eres un asistente útil",
            userMessage = "Hola, ¿puedes recomendarme algo?"
        )

        // Creamos ChatResponse simulados con deep stubs para poder mockear la cadena result.output.text
        val response1 = Mockito.mock(ChatResponse::class.java, Mockito.RETURNS_DEEP_STUBS)
        val response2 = Mockito.mock(ChatResponse::class.java, Mockito.RETURNS_DEEP_STUBS)

        // Stubear la cadena de texto que devolverá cada ChatResponse
        given(response1.result.output.text).willReturn("Hola, claro, te recomiendo ")
        given(response2.result.output.text).willReturn("probar Kotlin con Spring AI.")

        // Stubear chatModel.stream(...) para devolver un Flux con las respuestas simuladas
        given(chatModel.stream(any<Prompt>()))
            .willReturn(Flux.just(response1, response2))

        // When: recolectar todos los elementos del Flow y concatenarlos
        val parts = recommender.recommendStream(messageSuggestion).toList()
        val result = parts.joinToString(separator = "")

        // Then
        assertEquals(
            "Hola, claro, te recomiendo probar Kotlin con Spring AI.",
            result
        )

        verify(chatModel).stream(any<Prompt>())
    }

    @Test
    fun `recommend should return GenerationResultEntity with full response and mapped entity`() = runTest {
        // Given
        val messageSuggestion = MessageSuggestion(
            systemMessage = "Eres un asistente útil",
            userMessage = "Dame una recomendación"
        )

        val expectedEntity = MessageSuggestionEntity(
            systemMessage = messageSuggestion.systemMessage,
            userMessage = messageSuggestion.userMessage
        )

        // Creamos ChatResponse simulados
        val response1 = Mockito.mock(ChatResponse::class.java, Mockito.RETURNS_DEEP_STUBS)
        val response2 = Mockito.mock(ChatResponse::class.java, Mockito.RETURNS_DEEP_STUBS)

        given(response1.result.output.text).willReturn("Primera parte. ")
        given(response2.result.output.text).willReturn("Segunda parte.")

        given(chatModel.stream(any<Prompt>()))
            .willReturn(Flux.just(response1, response2))

        // Usar coincidencia por instancia para evitar problemas con matchers en Kotlin
        given(messageSuggestionMapper.toEntity(messageSuggestion))
            .willReturn(expectedEntity)

        // When
        val result = recommender.recommend(messageSuggestion)

        // Then
        assertEquals("Primera parte. Segunda parte.", result.fullResponse)
        assertEquals(expectedEntity, result.messageSuggestionEntity)

        verify(chatModel).stream(any<Prompt>())
        verify(messageSuggestionMapper).toEntity(messageSuggestion)
    }
}