package ru.mirea.framespace.domain;

/** Рассчитанный уровень приоритета заявки. */
public enum PriorityLevel {
    /** Приоритет не рассчитывается для завершённых или отменённых заявок. */
    NOT_APPLICABLE,
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}
