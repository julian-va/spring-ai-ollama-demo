package jva.cloud.infrastructure.adapters.output.ia.mapper

import jva.cloud.domain.model.MessageSuggestion
import jva.cloud.infrastructure.adapters.output.ia.entity.MessageSuggestionEntity
import org.mapstruct.Mapper

@Mapper(componentModel = "spring")
interface MessageSuggestionMapper {
    fun toModel(entity: MessageSuggestionEntity): MessageSuggestion
    fun toEntity(model: MessageSuggestion): MessageSuggestionEntity
}