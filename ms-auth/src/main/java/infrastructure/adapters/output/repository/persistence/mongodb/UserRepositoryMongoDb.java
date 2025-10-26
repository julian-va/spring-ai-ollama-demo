package infrastructure.adapters.output.repository.persistence.mongodb;

import application.port.input.UserRepositoryPort;
import domain.model.User;
import infrastructure.adapters.mappers.UserEntityMapper;
import infrastructure.adapters.output.repository.persistence.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@RequiredArgsConstructor
public class UserRepositoryMongoDb implements UserRepositoryPort {
    private final UserRepository userRepository;
    private final UserEntityMapper userEntityMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Mono<User> createUser(String email, String password) {

        return userRepository
                .save(UserEntity
                        .builder()
                        .username(email)
                        .password(passwordEncoder.encode(password))
                        .roles(List.of("USER"))
                        .build())
                .map(userEntityMapper::toDomain);
    }

    @Override
    public Mono<User> findFirstByEmail(String email) {
        return userRepository.findByUsername(email).map(userEntityMapper::toDomain);
    }
}
