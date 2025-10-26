package infrastructure.configuration;

import application.port.input.UserRepositoryPort;
import application.service.CreateUserService;
import application.usecase.CreateUserUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfiguration {
    @Bean
    public CreateUserUseCase createUserUseCaseBean(UserRepositoryPort userRepositoryPort) {
        return new CreateUserService(userRepositoryPort);
    }
}
