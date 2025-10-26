package infrastructure.adapters.mappers;

import domain.model.User;
import infrastructure.adapters.output.repository.persistence.entity.UserEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserEntityMapper {
    User toDomain(UserEntity userEntity);

    UserEntity toEntity(User user);
}
