package application.usecase;

import domain.model.AuthenticationResult;
import reactor.core.publisher.Mono;

public interface LoginUsaCase {
    Mono<AuthenticationResult> login(String username, String password);
}
