package infrastructure.configuration.security;

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

/**
 * Spring Security configuration for the reactive API gateway.
 *
 * <p>This configuration enables WebFlux security, configures route-level
 * authorization rules, and sets up JWT-based resource server support with a
 * custom converter that extracts realm roles from a token claim. The class
 * exposes beans used by Spring Security to perform authentication and
 * authorization in a reactive environment.</p>
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {
    private static final String PUBLIC_PATH = "/public/**";
    private static final String OLLAMA_USER_ROLE = "ollama-user";
    private static final String ROLE_PREFIX = "ROLE_";
    private static final String REALM_ACCESS_CLAIM = "realm_access";
    private static final String ROLES_CLAIM = "roles";

    /**
     * Configure the security filter chain for the gateway.
     *
     * <p>Rules applied:
     * <ul>
     *   <li>Requests under {@code /public/**} are permitted without authentication.</li>
     *   <li>All other requests require the {@code ollama-user} role.</li>
     * </ul>
     * The method also configures OAuth2 login and resource-server JWT support
     * that uses a custom JWT authentication converter.</p>
     *
     * @param http the ServerHttpSecurity to configure
     * @return the configured SecurityWebFilterChain
     */
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


    /**
     * Provide a reactive JWT authentication converter adapter that maps JWT
     * claims into granted authorities. The converter extracts realm roles from
     * the {@code realm_access.roles} claim and prefixes them with {@code ROLE_}.
     * If realm roles are not present, the default JwtGrantedAuthoritiesConverter
     * is used.
     *
     * @return a ReactiveJwtAuthenticationConverterAdapter instance
     */
    @Bean
    public ReactiveJwtAuthenticationConverterAdapter jwtReactiveAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter defaultConverter = new JwtGrantedAuthoritiesConverter();
        defaultConverter.setAuthorityPrefix(ROLE_PREFIX);

        Converter<Jwt, Collection<GrantedAuthority>> converter = buildRealmRolesConverter(defaultConverter);

        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(converter);

        return new ReactiveJwtAuthenticationConverterAdapter(jwtConverter);
    }


    /**
     * Build a reactive JWT decoder using the issuer URI configured for Keycloak.
     *
     * @param issuerUri the issuer URI from configuration
     * @return a ReactiveJwtDecoder that resolves keys from the issuer
     */
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
