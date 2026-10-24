package ru.mirea.kinostudio.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Управляет подключением к базе данных PostgreSQL.
 * Параметры (DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD) берутся из файла .env
 * или из переменных окружения (см. {@link AppConfig}) — в исходном коде их нет.
 */
public final class DatabaseManager {

    private DatabaseManager() {
    }

    public static Connection getConnection() throws SQLException {
        String user = AppConfig.get("DB_USER");
        String password = AppConfig.get("DB_PASSWORD");
        if (user == null || password == null) {
            throw new SQLException("В файле .env не заданы DB_USER и DB_PASSWORD", "08001");
        }
        return DriverManager.getConnection(AppConfig.dbUrl(), user, password);
    }

    /** URL подключения (без пароля) — для показа в интерфейсе. */
    public static String getUrl() {
        return AppConfig.dbUrl();
    }
}
