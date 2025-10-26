package application.port.output;

import domain.model.User;
import reactor.core.publisher.Mono;

public interface TokenServicePort {
    Mono<String> createAccessToken(User user);

    Mono<String> createRefreshToken(User user);

    Mono<Boolean> validateToken(String token);

    Mono<String> subject(String token);
}
