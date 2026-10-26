package ru.mirea.kinostudio.repository;

import ru.mirea.kinostudio.model.User;

import java.sql.SQLException;
import java.util.Optional;

/**
 * Хранилище сотрудников API. Реализация может быть JDBC или память для тестов.
 */
public interface UserStore {
    Optional<User> findByUsername(String username) throws SQLException;

    User save(User user) throws SQLException;
}
