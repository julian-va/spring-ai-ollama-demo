package infrastructure.adapters.mapper

import domain.model.GenerationResult
import infrastructure.adapters.dto.GenerationResultDto
import org.mapstruct.Mapper
import org.mapstruct.Mapping

/**
 * MapStruct mapper to convert between the domain [GenerationResult]
 * and the persistence [GenerationResultDto] representations.
 * Implemented by MapStruct at build time (component model: spring).
 */
@Mapper(componentModel = "spring", uses = [MessageSuggestionMapper::class])
interface GenerationResultMapper {
    /**
     * Convert a persistence entity to a domain model.
     */
    @Mapping(source = "requestMessageSuggestionDto", target = "messageSuggestion")
    fun toModel(entity: GenerationResultDto): GenerationResult

    /**
     * Convert a domain model to a persistence entity.
     */
    @Mapping(source = "messageSuggestion", target = "requestMessageSuggestionDto")
    fun toEntity(model: GenerationResult): GenerationResultDto
}