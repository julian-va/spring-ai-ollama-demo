package application.service;

import application.port.input.UserRepositoryPort;
import application.port.output.TokenServicePort;
import application.usecase.LoginUsaCase;
import domain.model.AuthenticationResult;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class LoginService implements LoginUsaCase {
    private final UserRepositoryPort userRepositoryPort;
    private final TokenServicePort tokenServicePort;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Mono<AuthenticationResult> login(String username, String password) {
        return userRepositoryPort
                .findFirstByEmail(username)
                .flatMap(user -> {
                    if (passwordEncoder.matches(password, user.getPassword())) {
                        Mono<String> accessTokenMono = tokenServicePort.createAccessToken(user);
                        Mono<String> refreshTokenMono = tokenServicePort.createRefreshToken(user);
                        return Mono.zip(accessTokenMono, refreshTokenMono)
                                .map(tokens -> AuthenticationResult.builder()
                                        .accessToken(tokens.getT1())
                                        .refreshToken(tokens.getT2())
                                        .build());
                    } else {
                        return Mono.error(new RuntimeException("Authentication failed"));
                    }
                });
    }
}
