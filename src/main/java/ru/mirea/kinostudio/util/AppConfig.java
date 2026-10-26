package ru.mirea.kinostudio.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;

/**
 * Настройки приложения: параметры БД и данные администратора.
 *
 * Порядок поиска значения:
 *   1) системное свойство (-DDB_PASSWORD=...);
 *   2) переменная окружения (так значения передаёт Docker);
 *   3) файл .env — ищется в текущей папке и в родительских (пароль не зашит в код и не лежит в jar).
 */
public final class AppConfig {

    private static final Logger LOG = Logger.getLogger(AppConfig.class.getName());
    private static Map<String, String> fileValues;

    private AppConfig() {
    }

    /** @return значение или null, если параметр нигде не задан */
    public static String get(String key) {
        String value = System.getProperty(key);
        if (value == null || value.isBlank()) value = System.getenv(key);
        if (value == null || value.isBlank()) value = fromFile().get(key);
        return value == null || value.isBlank() ? null : value;
    }

    public static String get(String key, String defaultValue) {
        String value = get(key);
        return value == null ? defaultValue : value;
    }

    /** Строка подключения собирается из DB_HOST, DB_PORT и DB_NAME (или берётся готовой из DB_URL). */
    public static String dbUrl() {
        String url = get("DB_URL");
        if (url != null) return url;
        return "jdbc:postgresql://" + get("DB_HOST", "localhost") + ":" + get("DB_PORT", "5432")
                + "/" + get("DB_NAME", "kinostudio_db");
    }

    private static synchronized Map<String, String> fromFile() {
        if (fileValues == null) {
            fileValues = new HashMap<>();
            Path dir = Paths.get("").toAbsolutePath();
            for (int i = 0; i < 4 && dir != null; i++, dir = dir.getParent()) {
                Path env = dir.resolve(".env");
                if (Files.isRegularFile(env)) {
                    parse(env);
                    break;
                }
            }
        }
        return fileValues;
    }

    private static void parse(Path file) {
        try {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            for (String raw : lines) {
                String line = raw.strip();
                if (line.isEmpty() || line.startsWith("#")) continue;
                int eq = line.indexOf('=');
                if (eq <= 0) continue;
                String key = line.substring(0, eq).strip();
                String value = line.substring(eq + 1).strip();
                if (value.length() >= 2 && (value.startsWith("\"") && value.endsWith("\"")
                        || value.startsWith("'") && value.endsWith("'"))) {
                    value = value.substring(1, value.length() - 1);
                }
                fileValues.put(key, value);
            }
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Не удалось прочитать файл .env: " + file, e);
        }
    }
}
