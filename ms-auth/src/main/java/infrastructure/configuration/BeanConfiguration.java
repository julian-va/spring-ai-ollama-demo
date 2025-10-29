package infrastructure.configuration;

import application.port.input.UserRepositoryPort;
import application.port.output.TokenServicePort;
import application.service.CreateUserService;
import application.service.LoginService;
import application.usecase.CreateUserUseCase;
import application.usecase.LoginUsaCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class BeanConfiguration {
    @Bean
    public CreateUserUseCase createUserUseCaseBean(UserRepositoryPort userRepositoryPort) {
        return new CreateUserService(userRepositoryPort);
    }

    @Bean
    public LoginUsaCase loginUseCaseBean(UserRepositoryPort userRepositoryPort, TokenServicePort tokenServicePort, PasswordEncoder passwordEncoder) {
        return new LoginService(userRepositoryPort, tokenServicePort, passwordEncoder);
    }
}
