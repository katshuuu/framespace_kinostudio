-- Сотрудник REST API. Не заказчик съёмки и не связан с clients.
-- Файл schema.sql эту таблицу не создаёт: клиент киностудии в систему не входит.
-- Повторный запуск безопасен. Демонстрационный вход admin / admin123
-- добавляет ApiServer, если такого логина ещё нет.

CREATE TABLE IF NOT EXISTS users (
    id            SERIAL PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    full_name     VARCHAR(150) NOT NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);
