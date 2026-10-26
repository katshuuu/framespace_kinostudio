package ru.mirea.kinostudio.exception;

/**
 * Нарушение уникальности, которое API отдаёт кодом 409.
 * Остаётся {@link BusinessException}, чтобы вызывающий код обрабатывал его вместе с правилами.
 */
public class ConflictException extends BusinessException {
    public ConflictException(String message) {
        super(message);
    }
}
