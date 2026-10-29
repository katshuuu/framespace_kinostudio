package ru.mirea.framespace.service;

import org.junit.jupiter.api.Test;
import ru.mirea.framespace.domain.PriorityLevel;
import ru.mirea.framespace.domain.RequestStatus;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/** Фиксирует бизнес-правила приоритета заявок. */
class PriorityCalculatorTest {
    private final PriorityCalculator calculator = new PriorityCalculator();

    @Test
    void urgentAndHighBudgetAreCritical() {
        assertThat(calculator.calculate(RequestStatus.NEW, new BigDecimal("500000"), 4, true))
                .isEqualTo(PriorityLevel.CRITICAL);
    }

    @Test
    void moderateBudgetWithoutUrgencyIsLow() {
        assertThat(calculator.calculate(RequestStatus.APPROVED, new BigDecimal("100000"), 4, false))
                .isEqualTo(PriorityLevel.LOW);
    }

    @Test
    void completedRequestHasNoPriority() {
        assertThat(calculator.calculate(RequestStatus.COMPLETED, new BigDecimal("900000"), 20, true))
                .isEqualTo(PriorityLevel.NOT_APPLICABLE);
    }
}
