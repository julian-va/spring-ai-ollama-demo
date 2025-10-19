package jva.cloud.application.service

import jva.cloud.domain.model.GenerationResult
import jva.cloud.domain.model.MessageSuggestion
import jva.cloud.domain.port.out.AiRecommenderPort
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Answers.RETURNS_DEEP_STUBS
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import org.springframework.ai.chat.model.ChatResponse

@ExtendWith(MockitoExtension::class)
class AiRecommenderServiceTest {

    @Mock
    lateinit var aiRecommenderPort: AiRecommenderPort

    @InjectMocks
    lateinit var service: AiRecommenderService

    @Test
    fun `recommendStream maps chat responses to text fragments`() = runTest {
        val suggestion = MessageSuggestion(systemMessage = "sys", userMessage = "hello")

        // Create ChatResponse mocks using deep stubs so we can stub nested getters
        val crA = Mockito.mock(ChatResponse::class.java, RETURNS_DEEP_STUBS)
        val crB = Mockito.mock(ChatResponse::class.java, RETURNS_DEEP_STUBS)

        // Stub nested properties: result.output.text
        whenever(crA.result.output.text).thenReturn("fragment1")
        whenever(crB.result.output.text).thenReturn("fragment2")

        whenever(aiRecommenderPort.sentModelStream(any())) doReturn flowOf(crA, crB)

        val flow = service.recommendStream(suggestion)
        val collected = mutableListOf<String>()
        flow.collect { collected.add(it) }

        assertEquals(listOf("fragment1", "fragment2"), collected)
    }

    @Test
    fun `recommend concatenates and formats response`() = runTest {
        val suggestion = MessageSuggestion(systemMessage = "sys", userMessage = "user")

        val crA = Mockito.mock(ChatResponse::class.java, RETURNS_DEEP_STUBS)
        val crB = Mockito.mock(ChatResponse::class.java, RETURNS_DEEP_STUBS)

        whenever(crA.result.output.text).thenReturn("Hello\n")
        whenever(crB.result.output.text).thenReturn("World")

        whenever(aiRecommenderPort.sentModelStream(any())) doReturn flowOf(crA, crB)

        val result: GenerationResult = service.recommend(suggestion)

        assertEquals(suggestion, result.messageSuggestion)
        assertTrue(result.durationMs >= 0)
        // The service concatenates the fragments and formats them (keeps newline normalization)
        assertTrue(result.fullResponse.contains("Hello"))
        assertTrue(result.fullResponse.contains("World"))
    }
}
