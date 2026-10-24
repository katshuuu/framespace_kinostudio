package ru.mirea.kinostudio.model;

/** Вошедший в систему пользователь. Хеш пароля сюда не попадает. */
public record User(int id, String login, Role role) {

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }
}
