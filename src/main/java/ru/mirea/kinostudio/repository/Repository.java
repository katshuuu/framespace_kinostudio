package ru.mirea.kinostudio.repository;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Обобщённый интерфейс репозитория (CRUD).
 * Реализуется UserRepository и ShootingRequestRepository (полиморфизм).
 */
public interface Repository<T> {
    T save(T entity) throws SQLException;
    Optional<T> findById(int id) throws SQLException;
    List<T> findAll() throws SQLException;
    void update(T entity) throws SQLException;
    void deleteById(int id) throws SQLException;
}
