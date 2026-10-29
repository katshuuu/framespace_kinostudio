package ru.mirea.framespace.domain;

import java.util.EnumSet;
import java.util.Set;

/**
 * Статус заявки на съёмку и граф допустимых переходов из КР №1/№2.
 *
 * <p>NEW → APPROVED | REJECTED; APPROVED → IN_PROGRESS | CANCELLED;
 * IN_PROGRESS → COMPLETED | CANCELLED; COMPLETED, REJECTED, CANCELLED — конечные.</p>
 */
public enum RequestStatus {
    NEW,
    APPROVED,
    IN_PROGRESS,
    COMPLETED,
    REJECTED,
    CANCELLED;

    /** Статусы, в которые можно перевести заявку из текущего. */
    public Set<RequestStatus> nextStatuses() {
        return switch (this) {
            case NEW -> EnumSet.of(APPROVED, REJECTED);
            case APPROVED -> EnumSet.of(IN_PROGRESS, CANCELLED);
            case IN_PROGRESS -> EnumSet.of(COMPLETED, CANCELLED);
            case COMPLETED, REJECTED, CANCELLED -> EnumSet.noneOf(RequestStatus.class);
        };
    }

    /** Повторная установка того же статуса переходом не считается и разрешена. */
    public boolean canMoveTo(RequestStatus target) {
        return this == target || nextStatuses().contains(target);
    }

    public boolean isFinal() {
        return nextStatuses().isEmpty();
    }
}
