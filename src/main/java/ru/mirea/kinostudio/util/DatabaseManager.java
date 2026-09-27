package ru.mirea.kinostudio.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Управляет подключением к базе данных PostgreSQL.
 * Измените константы под своё окружение (или вынесите в .properties при желании).
 */
public final class DatabaseManager {

    private static final String URL = "jdbc:postgresql://localhost:5432/kinostudio_db";
    private static final String USER = "postgres";
    private static final String PASSWORD = "postgres";

    private DatabaseManager() {
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
