package ru.mirea.kinostudio.exception;

/**
 * Выбрасывается при нарушении бизнес-правил предметной области.
 */
public class BusinessException extends Exception {
    public BusinessException(String message) {
        super(message);
    }
}
