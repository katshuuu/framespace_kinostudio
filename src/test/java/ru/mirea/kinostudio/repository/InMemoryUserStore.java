package ru.mirea.kinostudio.repository;

import ru.mirea.kinostudio.model.User;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class InMemoryUserStore implements UserStore {

    private final AtomicInteger sequence = new AtomicInteger();
    private final Map<String, User> byUsername = new ConcurrentHashMap<>();

    @Override
    public Optional<User> findByUsername(String username) {
        User found = byUsername.get(username);
        if (found == null) {
            return Optional.empty();
        }
        return Optional.of(copy(found));
    }

    @Override
    public User save(User user) {
        User stored = new User(
                sequence.incrementAndGet(),
                user.getUsername(),
                user.getPasswordHash(),
                user.getFullName(),
                LocalDateTime.now());
        byUsername.put(stored.getUsername(), stored);
        return copy(stored);
    }

    private static User copy(User user) {
        return new User(
                user.getId(),
                user.getUsername(),
                user.getPasswordHash(),
                user.getFullName(),
                user.getCreatedAt());
    }
}
