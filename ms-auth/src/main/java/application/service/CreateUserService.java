package application.service;

import application.port.input.UserRepositoryPort;
import application.usecase.CreateUserUseCase;
import domain.model.User;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class CreateUserService implements CreateUserUseCase {
    private final UserRepositoryPort userRepositoryPort;

    @Override
    public Mono<User> createUser(String email, String password) {
        return userRepositoryPort.createUser(email, password);
    }
}
