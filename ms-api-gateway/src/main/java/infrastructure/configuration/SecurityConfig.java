package infrastructure.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoders;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {
    private static final String PUBLIC_PATH = "/public/**";
    private static final String OLLAMA_USER_ROLE = "ollama-user";
    private static final String ROLE_PREFIX = "ROLE_";
    private static final String REALM_ACCESS_CLAIM = "realm_access";
    private static final String ROLES_CLAIM = "roles";

    @Bean
    public SecurityWebFilterChain securityFilterChain(ServerHttpSecurity http) {
        http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(PUBLIC_PATH).permitAll()
                        .anyExchange().hasRole(OLLAMA_USER_ROLE)
                )
                .oauth2Login(withDefaults())
                .oauth2ResourceServer(oauth2 ->
                        oauth2.jwt(jwtSpec -> jwtSpec.jwtAuthenticationConverter(jwtReactiveAuthenticationConverter()))
                );

        return http.build();
    }


    @Bean
    public ReactiveJwtAuthenticationConverterAdapter jwtReactiveAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter defaultConverter = new JwtGrantedAuthoritiesConverter();
        defaultConverter.setAuthorityPrefix(ROLE_PREFIX);

        Converter<Jwt, Collection<GrantedAuthority>> converter = buildRealmRolesConverter(defaultConverter);

        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(converter);

        return new ReactiveJwtAuthenticationConverterAdapter(jwtConverter);
    }


    @Bean
    public ReactiveJwtDecoder reactiveJwtDecoder(@Value("${spring.security.oauth2.client.provider.keycloak.issuer-uri}") String issuerUri) {
        return ReactiveJwtDecoders.fromIssuerLocation(issuerUri);
    }

    private Converter<Jwt, Collection<GrantedAuthority>> buildRealmRolesConverter(JwtGrantedAuthoritiesConverter defaultConverter) {
        return jwt -> {
            Object realmAccess = jwt.getClaim(REALM_ACCESS_CLAIM);
            if (realmAccess instanceof Map) {
                Object rolesObj = ((Map<?, ?>) realmAccess).get(ROLES_CLAIM);
                if (rolesObj instanceof Iterable) {
                    return StreamSupport.stream(((Iterable<?>) rolesObj).spliterator(), false)
                            .map(Object::toString)
                            .map(role -> new SimpleGrantedAuthority(ROLE_PREFIX + role))
                            .collect(Collectors.toList());
                }
            }
            return defaultConverter.convert(jwt);
        };
    }
}
