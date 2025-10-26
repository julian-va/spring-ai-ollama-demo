package infrastructure.adapters.mappers;

import domain.model.User;
import infrastructure.adapters.output.repository.persistence.entity.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserEntityMapper {

    // @Mapping(target = "roles", expression = "java(userEntity.getRoles())")
    User toDomain(UserEntity userEntity);

    @Mapping(source = "roles", target = "roles")
    UserEntity toEntity(User user);
}
