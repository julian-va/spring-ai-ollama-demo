package jva.cloud.domain.port.out

import jva.cloud.domain.model.MessageSuggestion
import kotlinx.coroutines.flow.Flow

interface AiRecommender {
    fun recommend(messageSuggestion: MessageSuggestion): Flow<String>
}