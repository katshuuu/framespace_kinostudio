package ru.mirea.kinostudio.repository;

import ru.mirea.kinostudio.model.Role;
import ru.mirea.kinostudio.model.User;
import ru.mirea.kinostudio.util.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

/** Доступ к таблице users (пользователи информационной системы). Весь SQL — только здесь. */
public class UserRepository {

    /** Пользователь вместе с хешем пароля — нужен только для проверки входа. */
    public record StoredUser(User user, String passwordHash) {
    }

    /** Создаёт таблицу, если её ещё нет (чтобы приложение работало и на уже существующей базе из КР №1). */
    public void ensureTable() throws SQLException {
        try (Connection c = DatabaseManager.getConnection(); Statement st = c.createStatement()) {
            st.execute("""
                    CREATE TABLE IF NOT EXISTS users (
                        id            SERIAL PRIMARY KEY,
                        login         VARCHAR(30)  NOT NULL,
                        password_hash VARCHAR(200) NOT NULL,
                        role          VARCHAR(10)  NOT NULL DEFAULT 'USER' CHECK (role IN ('USER','ADMIN')),
                        created_at    TIMESTAMP    NOT NULL DEFAULT NOW()
                    )""");
            st.execute("CREATE UNIQUE INDEX IF NOT EXISTS ux_users_login_lower ON users (LOWER(login))");
        }
    }

    public Optional<StoredUser> findByLogin(String login) throws SQLException {
        String sql = "SELECT id, login, password_hash, role FROM users WHERE LOWER(login) = LOWER(?)";
        try (Connection c = DatabaseManager.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, login);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                User u = new User(rs.getInt("id"), rs.getString("login"), Role.valueOf(rs.getString("role")));
                return Optional.of(new StoredUser(u, rs.getString("password_hash")));
            }
        }
    }

    public boolean existsByLogin(String login) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE LOWER(login) = LOWER(?)";
        try (Connection c = DatabaseManager.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, login);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public User save(String login, String passwordHash, Role role) throws SQLException {
        String sql = "INSERT INTO users (login, password_hash, role) VALUES (?, ?, ?) RETURNING id";
        try (Connection c = DatabaseManager.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, login);
            ps.setString(2, passwordHash);
            ps.setString(3, role.name());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return new User(rs.getInt(1), login, role);
            }
        }
    }
}
