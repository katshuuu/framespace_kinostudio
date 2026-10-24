package ru.mirea.kinostudio.fx;

import java.sql.SQLException;

/**
 * Перевод технических ошибок PostgreSQL в понятные пользователю сообщения.
 * Код SQLState стандартный: 23505 — нарушение уникальности, 23503 — внешний ключ и т.д.
 */
public final class DbErrors {

    private DbErrors() {
    }

    public static String describe(SQLException e) {
        String state = e.getSQLState() == null ? "" : e.getSQLState();
        String msg = e.getMessage() == null ? "" : e.getMessage();
        switch (state) {
            case "23505":
                if (msg.contains("clients_email_key")) return "Клиент с таким email уже существует.";
                if (msg.contains("locations_name_key")) return "Локация с таким названием уже существует.";
                return "Запись с такими данными уже существует.";
            case "23503":
                return "Операцию нельзя выполнить: запись связана с другими данными "
                        + "(например, у локации есть заявки).";
            case "23502":
                return "Не заполнено обязательное поле.";
            case "23514":
                return "Значение не проходит проверку ограничений базы данных.";
            case "3D000":
                return "База данных не найдена. Проверьте имя базы в .env.";
            case "28000":
            case "28P01":
                return "Не удалось войти в базу данных: неверное имя пользователя или пароль в .env.";
            default:
                if (state.startsWith("08")) {
                    return "Не удалось подключиться к базе данных. Проверьте, что PostgreSQL запущен, "
                            + "а адрес, порт, имя пользователя и пароль в .env указаны верно.";
                }
                return "Ошибка при работе с базой данных. Подробности записаны в журнал.";
        }
    }
}
