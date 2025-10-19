package infrastructure.adapters.input.web

import application.usecase.AiRecommenderUseCase
import domain.model.GenerationResult
import domain.model.MessageSuggestion
import infrastructure.adapters.entity.GenerationResultEntity
import infrastructure.adapters.entity.MessageSuggestionEntity
import infrastructure.adapters.mapper.GenerationResultMapper
import infrastructure.adapters.mapper.MessageSuggestionMapper
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient

@ExtendWith(MockitoExtension::class)
class AiRecommenderControllerTest {

    @Mock
    lateinit var recommender: AiRecommenderUseCase

    @Mock
    lateinit var resultMapper: GenerationResultMapper

    @Mock
    lateinit var messageSuggestionMapper: MessageSuggestionMapper

    @InjectMocks
    lateinit var controller: AiRecommenderController

    @Test
    fun `retrieveRecommendations returns stream from recommender`() {
        // Arrange
        val entity = MessageSuggestionEntity(systemMessage = "sys", userMessage = "user")
        val model = MessageSuggestion(systemMessage = "sys", userMessage = "user")

        whenever(messageSuggestionMapper.toModel(entity)).thenReturn(model)
        whenever(recommender.recommendStream(model)).thenReturn(flowOf("r1", "r2"))

        val client = WebTestClient.bindToController(controller).build()

        // Act & Assert
        val resultSpec = client.post()
            .uri("/ai/recommender/stream")
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


        // Assert body
        assertEquals(listOf("r1", "r2"), cleaned)

        // Verify interactions
        verify(messageSuggestionMapper).toModel(entity)
        verify(recommender).recommendStream(model)
    }

    @Test
    fun `retrieveFullRecommendation returns generation result entity`() = runTest {
        // Arrange
        val entity = MessageSuggestionEntity(systemMessage = "sys", userMessage = "user")
        val model = MessageSuggestion(systemMessage = "sys", userMessage = "user")
        val resultEntity = GenerationResultEntity(
            fullResponse = "full",
            durationMs = 123L,
            messageSuggestionEntity = entity
        )

        val resultModel = GenerationResult(
            fullResponse = "full",
            durationMs = 123L,
            messageSuggestion = model
        )

        whenever(messageSuggestionMapper.toModel(entity)).thenReturn(model)
        // recommend is suspend, must stub from a coroutine - returns domain model
        whenever(recommender.recommend(model)).thenReturn(resultModel)
        whenever(resultMapper.toEntity(resultModel)).thenReturn(resultEntity)

        val client = WebTestClient.bindToController(controller).build()

        // Act & Assert
        val resultSpec = client.post()
            .uri("/ai/recommender/sync")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(entity)
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)

        resultSpec.expectBody()
            .jsonPath("$.full_response").isEqualTo("full")
            .jsonPath("$.duration_ms").isEqualTo(123)
            .jsonPath("$.message_suggestion.user_message").isEqualTo("user")
            .jsonPath("$.message_suggestion.system_message").isEqualTo("sys")

        // Verify interactions
        verify(messageSuggestionMapper).toModel(entity)
        verify(recommender).recommend(model)
        verify(resultMapper).toEntity(resultModel)
    }
}