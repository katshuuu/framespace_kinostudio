-- ============================================================
-- Пользователи информационной системы (КР №2)
-- Пароль хранится только в виде хеша (PBKDF2). Администратор в таблице не хранится:
-- его логин и пароль задаются в файле .env (ADMIN_LOGIN, ADMIN_PASSWORD).
-- Приложение создаёт эту таблицу само, если её нет; файл нужен для развёртывания в Docker.
-- ============================================================

CREATE TABLE IF NOT EXISTS users (
    id            SERIAL PRIMARY KEY,
    login         VARCHAR(30)  NOT NULL,
    password_hash VARCHAR(200) NOT NULL,
    role          VARCHAR(10)  NOT NULL DEFAULT 'USER' CHECK (role IN ('USER','ADMIN')),
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_users_login_lower ON users (LOWER(login));
