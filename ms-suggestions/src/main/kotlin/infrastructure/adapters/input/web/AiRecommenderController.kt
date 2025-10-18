package jva.cloud.infrastructure.adapters.input.web

import jva.cloud.domain.port.out.AiRecommender
import jva.cloud.infrastructure.adapters.entity.MessageSuggestionEntity
import jva.cloud.infrastructure.adapters.mapper.MessageSuggestionMapper
import kotlinx.coroutines.flow.Flow
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(value = ["/ai/recommender"])
class AiRecommenderController(
    private val recommender: AiRecommender,
    private val messageSuggestionMapper: MessageSuggestionMapper
) {

    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun retrieveRecommendations(@RequestBody messageSuggestionEntity: MessageSuggestionEntity): Flow<String> {
        return recommender.recommend(messageSuggestionMapper.toModel(entity = messageSuggestionEntity))
    }
}