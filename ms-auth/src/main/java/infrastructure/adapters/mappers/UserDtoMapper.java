package infrastructure.adapters.mappers;

import domain.model.User;
import infrastructure.adapters.dto.UserDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserDtoMapper {
    UserDto toDto(User user);

    User toDomain(UserDto userDto);
}
