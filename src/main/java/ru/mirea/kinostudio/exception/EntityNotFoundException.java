package ru.mirea.kinostudio.exception;

/**
 * Выбрасывается, когда запись с указанным ID не найдена в БД.
 */
public class EntityNotFoundException extends Exception {
    public EntityNotFoundException(String message) {
        super(message);
    }
}
