package ru.mirea.framespace.service;

import org.springframework.stereotype.Component;
import ru.mirea.framespace.domain.PriorityLevel;
import ru.mirea.framespace.domain.RequestStatus;

import java.math.BigDecimal;

/** Единообразно рассчитывает приоритет заявки для всех клиентов. */
@Component
public class PriorityCalculator {
    private static final BigDecimal HIGH_BUDGET = new BigDecimal("500000");
    private static final BigDecimal MEDIUM_BUDGET = new BigDecimal("200000");

    public PriorityLevel calculate(RequestStatus status, BigDecimal budget, Integer durationHours, boolean urgent) {
        if (status == RequestStatus.COMPLETED
                || status == RequestStatus.REJECTED
                || status == RequestStatus.CANCELLED) {
            return PriorityLevel.NOT_APPLICABLE;
        }
        BigDecimal effectiveBudget = budget == null ? BigDecimal.ZERO : budget;
        int effectiveDuration = durationHours == null ? 0 : durationHours;

        if (urgent && effectiveBudget.compareTo(HIGH_BUDGET) >= 0) {
            return PriorityLevel.CRITICAL;
        }
        if (urgent || effectiveBudget.compareTo(HIGH_BUDGET) >= 0 || effectiveDuration >= 16) {
            return PriorityLevel.HIGH;
        }
        if (effectiveBudget.compareTo(MEDIUM_BUDGET) >= 0 || effectiveDuration >= 8) {
            return PriorityLevel.MEDIUM;
        }
        return PriorityLevel.LOW;
    }
}
