package ru.mirea.framespace.error;

/** Сигнал отсутствия запрошенной сущности. */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) { super(message); }
}
