package jva.cloud.infrastructure.adapters.input.web

import jva.cloud.domain.port.out.AiRecommender
import jva.cloud.infrastructure.adapters.entity.GenerationResultEntity
import jva.cloud.infrastructure.adapters.entity.MessageSuggestionEntity
import jva.cloud.infrastructure.adapters.mapper.MessageSuggestionMapper
import kotlinx.coroutines.flow.Flow
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * REST controller that exposes the AI recommender endpoints.
 *
 * Provides a streaming endpoint for incremental responses and a synchronous
 * endpoint that returns the full formatted generation result.
 *
 * Dependencies:
 * - [AiRecommender]: the domain port implementing recommendation logic.
 * - [MessageSuggestionMapper]: maps incoming DTOs to domain models.
 */
@RestController
@RequestMapping(value = ["/ai/recommender"])
class AiRecommenderController(
    private val recommender: AiRecommender,
    private val messageSuggestionMapper: MessageSuggestionMapper
) {

    /**
     * Stream model-generated text fragments as Server-Sent Events (text/event-stream).
     *
     * @param messageSuggestionEntity incoming DTO with system and user messages.
     * @return a Flow of text fragments emitted by the model.
     */
    @PostMapping(
        value = ["/stream"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.TEXT_EVENT_STREAM_VALUE]
    )
    fun retrieveRecommendations(@RequestBody messageSuggestionEntity: MessageSuggestionEntity): Flow<String> {
        return recommender.recommendStream(messageSuggestionMapper.toModel(entity = messageSuggestionEntity))
    }

    /**
     * Produce a full recommendation synchronously and return it as JSON.
     *
     * @param messageSuggestionEntity incoming DTO with system and user messages.
     * @return HTTP 200 with the generated result entity.
     */
    @PostMapping(
        value = ["/sync"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    suspend fun retrieveFullRecommendation(@RequestBody messageSuggestionEntity: MessageSuggestionEntity): ResponseEntity<GenerationResultEntity> {
        val result: GenerationResultEntity =
            recommender.recommend(messageSuggestionMapper.toModel(entity = messageSuggestionEntity))
        return ResponseEntity.ok(result)
    }
}