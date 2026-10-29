package ru.mirea.framespace.filter;

import ru.mirea.framespace.domain.PriorityLevel;
import ru.mirea.framespace.domain.RequestStatus;

import java.math.BigDecimal;
import java.util.List;

/** Результат безопасного разбора строки умного фильтра. */
public record RequestFilter(
        String freeText,
        String location,
        String client,
        RequestStatus status,
        Boolean urgent,
        PriorityLevel priority,
        String tag,
        Boolean archived,
        List<BudgetConstraint> budgets) {

    public enum Operator { LT, LTE, EQ, GTE, GT }
    public record BudgetConstraint(Operator operator, BigDecimal value) { }
}
