package infrastructure.adapters.output.repository.persistence.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "users")
@ToString(exclude = "password")
public class UserEntity implements UserDetails {
    @Getter
    @Id
    private String id;
    @Indexed(unique = true)
    private String username;
    private String password;
    @Getter
    @Builder.Default
    private List<String> roles = new ArrayList<>();
    @Getter
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    @Getter
    private LocalDateTime updatedAt;


    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Objects.nonNull(roles) && !roles.isEmpty()
                ? roles.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList())
                : List.of();
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public String getUsername() {
        return this.username;
    }

}
