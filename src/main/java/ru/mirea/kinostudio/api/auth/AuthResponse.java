package ru.mirea.kinostudio.api.auth;

/**
 * Успешный ответ регистрации и входа.
 */
public record AuthResponse(
        String token,
        String tokenType,
        long expiresIn,
        int id,
        String username,
        String fullName) {
}
