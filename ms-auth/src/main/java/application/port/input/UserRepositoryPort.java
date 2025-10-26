package application.port.input;

import domain.model.User;
import reactor.core.publisher.Mono;

public interface UserRepositoryPort {
    Mono<User> createUser(String email, String password);

    Mono<User> findFirstByEmail(String email);
}
