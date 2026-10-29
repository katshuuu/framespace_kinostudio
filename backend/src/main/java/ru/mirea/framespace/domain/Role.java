package ru.mirea.framespace.domain;

/**
 * Роли прикладного доступа. Сервер проверяет их независимо от того, какие кнопки
 * скрыл конкретный клиент.
 */
public enum Role {
    VIEWER,
    OPERATOR,
    ADMIN
}
