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
import reactor.core.scheduler.Schedulers;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtTokenService implements TokenServicePort {
    private final JWKSource<SecurityContext> jwkSource;

    private static final String NO_JWK_AVAILABLE = "No JWK available for signing";
    private static final String ERROR_SIGNING_JWT = "Error signing JWT";

    // claims
    private static final String CLAIM_EMAIL = "username";
    private static final String CLAIM_TOKEN_TYPE = "token_type";

    // token types
    private static final String TOKEN_TYPE_ACCESS = "access";
    private static final String TOKEN_TYPE_REFRESH = "refresh";

    private final long accessTokenMs = 60 * 60 * 1000; // 1h
    private final long refreshTokenMs = 4 * 60 * 60 * 1000L; // 4h

    @Override
    public Mono<String> createAccessToken(User user) {
        return Mono.fromSupplier(() -> buildToken(user.getId(), user.getUsername(), accessTokenMs, TOKEN_TYPE_ACCESS))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<String> createRefreshToken(User user) {
        return Mono.fromSupplier(() -> buildToken(user.getId(), user.getUsername(), refreshTokenMs, TOKEN_TYPE_REFRESH))
                .subscribeOn(Schedulers.boundedElastic());
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


    private String buildToken(String subject, String email, long accessTokenMs, String tokenType) {
        try {
            // Seleccionar clave RSA para firma
            JWKSelector selector = new JWKSelector(new JWKMatcher.Builder()
                    .keyType(KeyType.RSA)
                    .keyUse(KeyUse.SIGNATURE)
                    .build());
            SecurityContext ctx = new SecurityContext() {
            };
            List<JWK> jwks = jwkSource.get(selector, ctx);
            if (jwks.isEmpty()) throw new IllegalStateException(NO_JWK_AVAILABLE);

            RSAKey rsaKey = (RSAKey) jwks.getFirst();
            RSAPrivateKey privateKey = rsaKey.toRSAPrivateKey();

            // Construir claims
            Date now = new Date();
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(subject)
                    .claim(CLAIM_EMAIL, email)
                    .claim(CLAIM_TOKEN_TYPE, tokenType)
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
            throw new RuntimeException(ERROR_SIGNING_JWT, e);
        }
    }
}
