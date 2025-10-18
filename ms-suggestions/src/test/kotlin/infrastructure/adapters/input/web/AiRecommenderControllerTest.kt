package infrastructure.adapters.input.web

import jva.cloud.domain.model.MessageSuggestion
import jva.cloud.domain.port.out.AiRecommender
import jva.cloud.infrastructure.adapters.entity.MessageSuggestionEntity
import jva.cloud.infrastructure.adapters.input.web.AiRecommenderController
import jva.cloud.infrastructure.adapters.mapper.MessageSuggestionMapper
import kotlinx.coroutines.flow.flowOf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient

@ExtendWith(MockitoExtension::class)
class AiRecommenderControllerTest {

    @Mock
    lateinit var recommender: AiRecommender

    @Mock
    lateinit var messageSuggestionMapper: MessageSuggestionMapper

    @Test
    fun `retrieveRecommendations returns stream from recommender`() {
        // Arrange
        val entity = MessageSuggestionEntity(systemMessage = "sys", userMessage = "user")
        val model = MessageSuggestion(systemMessage = "sys", userMessage = "user")

        whenever(messageSuggestionMapper.toModel(entity)).thenReturn(model)
        whenever(recommender.recommend(model)).thenReturn(flowOf("r1", "r2"))

        val controller = AiRecommenderController(recommender, messageSuggestionMapper)
        val client = WebTestClient.bindToController(controller).build()

        // Act & Assert
        val resultSpec = client.post()
            .uri("/ai/recommender")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.TEXT_EVENT_STREAM)
            .bodyValue(entity)
            .exchange()
            .expectStatus().isOk
            // Accept header with optional charset (server may add charset=UTF-8)
            .expectHeader().valueMatches("Content-Type", "text/event-stream.*")

        val response = resultSpec.returnResult(String::class.java)
        val resultList = response.responseBody.collectList().block()

        // Normalize SSE payloads like "data: r1" or with extra whitespace/newlines
        val cleaned = resultList
            ?.map { it.trim() }
            ?.map { if (it.startsWith("data:")) it.removePrefix("data:").trim() else it }
            ?.filter { it.isNotEmpty() }

        // cleaned now contains the SSE data payloads

        // Assert body
        assertEquals(listOf("r1", "r2"), cleaned)

        // Verify interactions
        verify(messageSuggestionMapper).toModel(entity)
        verify(recommender).recommend(model)
    }
}