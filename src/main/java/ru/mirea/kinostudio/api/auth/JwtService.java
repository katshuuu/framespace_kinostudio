package ru.mirea.kinostudio.api.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;

import java.time.Instant;
import java.util.Date;

/**
 * Выдаёт и проверяет JWT с подписью HMAC-SHA256.
 * Секрет приходит извне и в код не записывается.
 */
public final class JwtService {

    private final Algorithm algorithm;
    private final long ttlSeconds;

    public JwtService(String secret, long ttlSeconds) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalArgumentException("JWT_SECRET должен содержать не меньше 32 символов.");
        }
        this.algorithm = Algorithm.HMAC256(secret);
        this.ttlSeconds = ttlSeconds;
    }

    public long ttlSeconds() {
        return ttlSeconds;
    }

    public String issue(int userId, String username) {
        Instant now = Instant.now();
        return JWT.create()
                .withSubject(Integer.toString(userId))
                .withClaim("username", username)
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(now.plusSeconds(ttlSeconds)))
                .sign(algorithm);
    }

    public DecodedJWT verify(String token) {
        return JWT.require(algorithm).build().verify(token);
    }
}
