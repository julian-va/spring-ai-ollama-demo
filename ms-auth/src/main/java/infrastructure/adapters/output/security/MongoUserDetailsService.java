package infrastructure.adapters.output.security;

import infrastructure.adapters.output.repository.persistence.mongodb.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MongoUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .map(user -> User.builder()
                        .username(user.getUsername())
                        .password(user.getPassword())
                        .roles(user.getRoles() == null ? new String[0] : user.getRoles().toArray(new String[0]))
                        .build())
                .blockOptional()
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
    }
}
