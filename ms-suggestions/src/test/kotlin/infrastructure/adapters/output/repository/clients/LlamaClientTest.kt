package infrastructure.adapters.output.repository.clients

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.ai.chat.messages.SystemMessage
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.ollama.OllamaChatModel
import reactor.core.publisher.Flux

@ExtendWith(MockitoExtension::class)
class LlamaClientTest {

    @Mock
    lateinit var chatModel: OllamaChatModel

    @InjectMocks
    lateinit var client: LlamaClient

    @Test
    fun `sentModelStream delegates to chatModel_stream and converts to Flow`() = runTest {
        // Arrange - build prompt messages
        val messages = listOf(SystemMessage("system"), UserMessage("user"))

        // Prepare ChatResponse mocks
        val cr1 = Mockito.mock(ChatResponse::class.java)
        val cr2 = Mockito.mock(ChatResponse::class.java)

        // Stub the reactive stream from the chatModel
        whenever(chatModel.stream(Mockito.any(Prompt::class.java))).thenReturn(Flux.just(cr1, cr2))

        // Act - call the SUT directly
        val collected = client.sentModelStream(messages).toList()

        // Assert - the flow yields the same elements from the Flux
        assertEquals(2, collected.size)
        assertSame(cr1, collected[0])
        assertSame(cr2, collected[1])

        // Verify delegation to the chatModel
        verify(chatModel).stream(Mockito.any(Prompt::class.java))
    }
}
