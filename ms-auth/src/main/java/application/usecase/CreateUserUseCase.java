package application.usecase;

import domain.model.User;
import reactor.core.publisher.Mono;

public interface CreateUserUseCase {
    Mono<User> createUser(String email, String password);
}
