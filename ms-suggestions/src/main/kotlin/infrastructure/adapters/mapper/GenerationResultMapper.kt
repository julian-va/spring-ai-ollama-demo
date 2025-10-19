package jva.cloud.infrastructure.adapters.mapper

import jva.cloud.domain.model.GenerationResult
import jva.cloud.infrastructure.adapters.entity.GenerationResultEntity
import org.mapstruct.Mapper
import org.mapstruct.Mapping

/**
 * MapStruct mapper to convert between the domain [GenerationResult]
 * and the persistence [GenerationResultEntity] representations.
 * Implemented by MapStruct at build time (component model: spring).
 */
@Mapper(componentModel = "spring", uses = [MessageSuggestionMapper::class])
interface GenerationResultMapper {
    /**
     * Convert a persistence entity to a domain model.
     */
    @Mapping(source = "messageSuggestionEntity", target = "messageSuggestion")
    fun toModel(entity: GenerationResultEntity): GenerationResult

    /**
     * Convert a domain model to a persistence entity.
     */
    @Mapping(source = "messageSuggestion", target = "messageSuggestionEntity")
    fun toEntity(model: GenerationResult): GenerationResultEntity
}