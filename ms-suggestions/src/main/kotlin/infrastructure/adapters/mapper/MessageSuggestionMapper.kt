package jva.cloud.infrastructure.adapters.mapper

import jva.cloud.domain.model.MessageSuggestion
import jva.cloud.infrastructure.adapters.entity.MessageSuggestionEntity
import org.mapstruct.Mapper

@Mapper(componentModel = "spring")
interface MessageSuggestionMapper {
    fun toModel(entity: MessageSuggestionEntity): MessageSuggestion
    fun toEntity(model: MessageSuggestion): MessageSuggestionEntity
}