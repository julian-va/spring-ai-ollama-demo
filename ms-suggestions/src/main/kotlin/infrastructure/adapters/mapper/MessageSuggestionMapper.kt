package infrastructure.adapters.mapper

import domain.model.MessageSuggestion
import infrastructure.adapters.entity.MessageSuggestionEntity
import org.mapstruct.Mapper

/**
 * MapStruct mapper to convert between domain [MessageSuggestion] and the
 * transport/persistence [MessageSuggestionEntity].
 */
@Mapper(componentModel = "spring")
interface MessageSuggestionMapper {
    /**
     * Convert a persistence/entity representation into the domain model.
     */
    fun toModel(entity: MessageSuggestionEntity): MessageSuggestion

    /**
     * Convert a domain model into the persistence/entity representation.
     */
    fun toEntity(model: MessageSuggestion): MessageSuggestionEntity
}