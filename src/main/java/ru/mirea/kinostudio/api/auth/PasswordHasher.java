package ru.mirea.kinostudio.api.auth;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Хеширует пароль перед записью. В базе и в токене исходный пароль не появляется.
 */
public final class PasswordHasher {

    private final int cost;

    public PasswordHasher() {
        this(12);
    }

    public PasswordHasher(int cost) {
        this.cost = cost;
    }

    public String hash(String rawPassword) {
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt(cost));
    }

    public boolean matches(String rawPassword, String passwordHash) {
        return BCrypt.checkpw(rawPassword, passwordHash);
    }
}
