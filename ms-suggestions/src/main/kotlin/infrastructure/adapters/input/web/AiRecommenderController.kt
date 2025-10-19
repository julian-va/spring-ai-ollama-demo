package infrastructure.adapters.input.web

import application.port.output.AiRecommenderPort
import application.usecase.AiRecommenderUseCase
import infrastructure.adapters.entity.GenerationResultEntity
import infrastructure.adapters.entity.MessageSuggestionEntity
import infrastructure.adapters.mapper.GenerationResultMapper
import infrastructure.adapters.mapper.MessageSuggestionMapper
import jakarta.validation.Valid
import kotlinx.coroutines.flow.Flow
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
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
 * - [AiRecommenderPort]: the domain port implementing recommendation logic.
 * - [MessageSuggestionMapper]: maps incoming DTOs to domain models.
 */
@RestController
@Validated
@RequestMapping(value = ["/ai/recommender"])
class AiRecommenderController(
    private val messageSuggestionMapper: MessageSuggestionMapper,
    private val resultMapper: GenerationResultMapper,
    private val aiRecommenderUseCase: AiRecommenderUseCase
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
    fun retrieveRecommendations(@Valid @RequestBody messageSuggestionEntity: MessageSuggestionEntity): Flow<String> {
        return aiRecommenderUseCase.recommendStream(messageSuggestionMapper.toModel(entity = messageSuggestionEntity))
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
    suspend fun retrieveFullRecommendation(@Valid @RequestBody messageSuggestionEntity: MessageSuggestionEntity): ResponseEntity<GenerationResultEntity> {
        val result: GenerationResultEntity =
            resultMapper.toEntity(aiRecommenderUseCase.recommend(messageSuggestionMapper.toModel(entity = messageSuggestionEntity)))
        return ResponseEntity.ok(result)
    }
}