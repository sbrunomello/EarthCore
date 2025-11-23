package mello.web;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;

import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Small helper around java-jwt to issue and validate short-lived tokens for the embedded frontend.
 */
public class JwtService {

    private static final long EXPIRATION_SECONDS = 24 * 60 * 60;

    private final Algorithm algorithm;
    private final String issuer;
    private final Logger logger;

    public JwtService(String secret, String issuer, Logger logger) {
        this.algorithm = Algorithm.HMAC256(secret);
        this.issuer = issuer;
        this.logger = logger;
    }

    public String issueToken(UUID uuid, String username) {
        Instant now = Instant.now();
        return JWT.create()
                .withIssuer(issuer)
                .withClaim("uuid", uuid.toString())
                .withClaim("username", username)
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(now.plusSeconds(EXPIRATION_SECONDS)))
                .sign(algorithm);
    }

    public Optional<DecodedJWT> verify(String token) {
        try {
            return Optional.of(JWT.require(algorithm).withIssuer(issuer).build().verify(token));
        } catch (Exception ex) {
            logger.log(Level.WARNING, "[Frontend] Token JWT inválido", ex);
            return Optional.empty();
        }
    }
}
