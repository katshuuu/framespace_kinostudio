package ru.mirea.kinostudio.api;

import ru.mirea.kinostudio.exception.BusinessException;
import ru.mirea.kinostudio.exception.ConflictException;
import ru.mirea.kinostudio.exception.EntityNotFoundException;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;

final class ApiResponses {

    private ApiResponses() {
    }

    static void json(HttpExchange exchange, int status, Object body) throws IOException {
        byte[] bytes = ApiJson.write(body);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.getResponseBody().close();
    }

    static void noContent(HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(204, -1);
    }

    static void methodNotAllowed(HttpExchange exchange) throws IOException {
        json(exchange, 405, Map.of("error", "Метод не поддерживается."));
    }

    static void sendError(HttpExchange exchange, Exception error) throws IOException {
        int status = 500;
        String message = "Внутренняя ошибка сервера.";
        if (error instanceof ConflictException) {
            status = 409;
            message = error.getMessage();
        } else if (error instanceof BusinessException || error instanceof IllegalArgumentException) {
            status = 400;
            message = error.getMessage();
        } else if (error instanceof EntityNotFoundException) {
            status = 404;
            message = error.getMessage();
        } else if (error instanceof DateTimeParseException) {
            status = 400;
            message = "Некорректная дата. Ожидается формат гггг-мм-дд.";
        } else if (error instanceof SQLException sql) {
            if (isUniqueViolation(sql)) {
                status = 409;
                message = "Значение уникального поля уже занято.";
            } else {
                message = "Ошибка SQL: " + sql.getMessage();
            }
        }
        if (status >= 500) {
            error.printStackTrace(System.err);
        }
        json(exchange, status, Map.of("error", message == null ? "Ошибка" : message));
    }

    static Map<String, String> query(HttpExchange exchange) {
        String raw = exchange.getRequestURI().getRawQuery();
        Map<String, String> result = new LinkedHashMap<>();
        if (raw == null || raw.isBlank()) {
            return result;
        }
        for (String pair : raw.split("&")) {
            if (pair.isEmpty()) {
                continue;
            }
            int eq = pair.indexOf('=');
            String key = decode(eq < 0 ? pair : pair.substring(0, eq));
            String value = decode(eq < 0 ? "" : pair.substring(eq + 1));
            result.putIfAbsent(key, value);
        }
        return result;
    }

    static int id(String path, String prefix) throws BusinessException {
        if (!path.startsWith(prefix)) {
            throw new BusinessException("Некорректный идентификатор.");
        }
        String raw = path.substring(prefix.length());
        if (raw.isEmpty() || !raw.chars().allMatch(Character::isDigit)) {
            throw new BusinessException("Некорректный идентификатор.");
        }
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            throw new BusinessException("Некорректный идентификатор.");
        }
    }

    static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static boolean isUniqueViolation(SQLException sql) {
        SQLException current = sql;
        while (current != null) {
            if ("23505".equals(current.getSQLState())) {
                return true;
            }
            current = current.getNextException();
        }
        return false;
    }
}
