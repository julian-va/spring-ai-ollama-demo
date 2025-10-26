package infrastructure.adapters.output.security;

import application.port.output.TokenServicePort;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtTokenService implements TokenServicePort {
    private final JWKSource<SecurityContext> jwkSource;

    private final long accessTokenMs = 60 * 60 * 1000; // 1h
    private final long refreshTokenMs = 4 * 60 * 60 * 1000L; // 4h

    @Override
    public Mono<String> createAccessToken(User user) {
        return Mono.fromSupplier(() -> buildToken(user.getId(), user.getEmail(), accessTokenMs));
    }

    @Override
    public Mono<String> createRefreshToken(User user) {
        return Mono.fromSupplier(() -> buildToken(user.getId(), user.getEmail(), refreshTokenMs));
    }

    @Override
    public Mono<Boolean> validateToken(String token) {
        return Mono.fromSupplier(() -> {
            try {
                SignedJWT jwt = SignedJWT.parse(token);

                // obtener kid del header
                JWSHeader header = jwt.getHeader();
                String kid = header.getKeyID();
                if (kid == null || kid.isBlank()) return false;

                // buscar la JWK RSA por kid
                JWKSelector selector = new JWKSelector(new JWKMatcher.Builder()
                        .keyID(kid)
                        .keyType(KeyType.RSA)
                        .keyUse(KeyUse.SIGNATURE)
                        .build());
                SecurityContext ctx = new SecurityContext() {
                };
                List<JWK> jwks = jwkSource.get(selector, ctx);
                if (jwks.isEmpty()) return false;

                RSAKey rsaKey = (RSAKey) jwks.getFirst();
                RSAPublicKey publicKey = rsaKey.toRSAPublicKey();

                // verificar firma
                JWSVerifier verifier = new RSASSAVerifier(publicKey);
                if (!jwt.verify(verifier)) return false;

                // comprobar expiración
                Date exp = jwt.getJWTClaimsSet().getExpirationTime();
                return exp != null && exp.after(new Date());
            } catch (Exception e) {
                return false;
            }
        });
    }

    @Override
    public Mono<String> subject(String token) {
        return Mono.fromSupplier(() -> {
            try {
                SignedJWT jwt = SignedJWT.parse(token);
                return jwt.getJWTClaimsSet().getSubject();
            } catch (Exception e) {
                return null;
            }
        });
    }


    private String buildToken(String subject, String email, long accessTokenMs) {
        try {
            // Seleccionar clave RSA para firma
            JWKSelector selector = new JWKSelector(new JWKMatcher.Builder()
                    .keyType(KeyType.RSA)
                    .keyUse(KeyUse.SIGNATURE)
                    .build());
            SecurityContext ctx = new SecurityContext() {
            };
            List<JWK> jwks = jwkSource.get(selector, ctx);
            if (jwks.isEmpty()) throw new IllegalStateException("No JWK disponible para firma");

            RSAKey rsaKey = (RSAKey) jwks.getFirst();
            RSAPrivateKey privateKey = rsaKey.toRSAPrivateKey();

            // Construir claims
            Date now = new Date();
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(subject)
                    .claim("email", email)
                    .issueTime(now)
                    .expirationTime(new Date(now.getTime() + accessTokenMs))
                    .build();

            // Firmar con RS256 usando la clave privada del JWK
            JWSSigner signer = new RSASSASigner(privateKey);
            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                    .keyID(rsaKey.getKeyID())
                    .type(JOSEObjectType.JWT)
                    .build();

            SignedJWT signedJWT = new SignedJWT(header, claims);
            signedJWT.sign(signer);
            return signedJWT.serialize();
        } catch (Exception e) {
            throw new RuntimeException("Error firmando JWT", e);
        }
    }
}
