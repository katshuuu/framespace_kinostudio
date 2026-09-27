package ru.mirea.kinostudio.model;

/**
 * Статус заявки на съёмку.
 */
public enum RequestStatus {
    NEW,          // новая заявка
    APPROVED,     // согласована
    IN_PROGRESS,  // съёмка идёт
    COMPLETED,    // завершена
    REJECTED,     // отклонена
    CANCELLED;    // отменена

    public static RequestStatus fromString(String value) {
        try {
            return RequestStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("Некорректный статус: " + value);
        }
    }
}
