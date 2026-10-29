package ru.mirea.framespace.domain;

/** Статус заявки на съёмку. */
public enum RequestStatus {
    NEW,
    APPROVED,
    IN_PROGRESS,
    COMPLETED,
    REJECTED,
    CANCELLED
}
