package infrastructure.adapters.dto;

import java.time.LocalDateTime;
import java.util.List;

public record UserDto(
        String id,
        String username,
        String password,
        List<String> roles,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
