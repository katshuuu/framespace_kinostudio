package ru.mirea.kinostudio.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Управляет подключением к базе данных PostgreSQL.
 * Параметры (url, пользователь, пароль) читаются из файла resources/database.properties,
 * а не зашиты в исходный код. Их можно переопределить системными свойствами
 * (-Ddb.url=..., -Ddb.user=..., -Ddb.password=...).
 */
public final class DatabaseManager {

    private static final String RESOURCE = "/database.properties";
    private static Properties props;

    private DatabaseManager() {
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(get("db.url"), get("db.user"), get("db.password"));
    }

    /** URL подключения (без пароля) — для показа в интерфейсе. */
    public static String getUrl() {
        try {
            return get("db.url");
        } catch (SQLException e) {
            return "не настроено";
        }
    }

    private static String get(String key) throws SQLException {
        String value = System.getProperty(key, load().getProperty(key));
        if (value == null) {
            throw new SQLException("В database.properties не задан параметр " + key, "08001");
        }
        return value;
    }

    private static synchronized Properties load() throws SQLException {
        if (props == null) {
            Properties p = new Properties();
            try (InputStream in = DatabaseManager.class.getResourceAsStream(RESOURCE)) {
                if (in != null) {
                    p.load(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8));
                }
            } catch (IOException e) {
                throw new SQLException("Не удалось прочитать database.properties: " + e.getMessage(), "08001", e);
            }
            props = p;
        }
        return props;
    }
}
