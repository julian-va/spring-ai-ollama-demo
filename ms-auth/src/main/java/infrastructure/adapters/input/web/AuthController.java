package infrastructure.adapters.input.web;

import application.usecase.CreateUserUseCase;
import infrastructure.adapters.dto.CreateUserDto;
import infrastructure.adapters.dto.UserDto;
import infrastructure.adapters.mappers.UserDtoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final CreateUserUseCase createUserUseCase;
    private final UserDtoMapper userDtoMapper;

    @PostMapping("/register")
    public Mono<ResponseEntity<UserDto>> createUser(@RequestBody CreateUserDto createUserDto) {
        return createUserUseCase
                .createUser(createUserDto.email(), createUserDto.password())
                .map(userDtoMapper::toDto)
                .map(ResponseEntity::ok);
    }

    @PostMapping("/login")
    public Mono<ResponseEntity<String>> login() {
        // Authentication is handled by Spring Security, so this endpoint can be empty
        return Mono.just(ResponseEntity.ok("Login successful"));
    }
}
