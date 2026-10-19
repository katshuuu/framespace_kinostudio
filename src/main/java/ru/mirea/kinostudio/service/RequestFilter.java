package ru.mirea.kinostudio.service;

import ru.mirea.kinostudio.model.RequestStatus;
import ru.mirea.kinostudio.model.RequestView;

import java.time.LocalDate;
import java.util.Locale;

/**
 * Условия поиска и фильтрации заявок. Все поля необязательные: null / пустая строка = «не фильтровать».
 *
 * Сравнение строк выполняется в Java (toLowerCase(Locale.ROOT)), а не в SQL через ILIKE:
 * у базы локаль C, и ILIKE не приводит к одному регистру русские буквы («Клип» и «клип»).
 */
public final class RequestFilter {

    private final String text;
    private final RequestStatus status;
    private final Integer locationId;
    private final LocalDate from;
    private final LocalDate to;

    public RequestFilter(String text, RequestStatus status, Integer locationId, LocalDate from, LocalDate to) {
        this.text = text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
        this.status = status;
        this.locationId = locationId;
        this.from = from;
        this.to = to;
    }

    /** Диапазон дат задан неверно: «с» позже «по». */
    public boolean hasInvalidDateRange() {
        return from != null && to != null && from.isAfter(to);
    }

    public boolean matches(RequestView v) {
        if (status != null && v.getStatus() != status) return false;
        if (locationId != null && v.getRequest().getLocationId() != locationId) return false;
        if (from != null && v.getShootingDate().isBefore(from)) return false;
        if (to != null && v.getShootingDate().isAfter(to)) return false;
        if (text.isEmpty()) return true;
        // Поиск сразу по нескольким смысловым полям
        return contains(v.getTitle()) || contains(v.getDescription())
                || contains(v.getClientName()) || contains(v.getLocationName());
    }

    private boolean contains(String value) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(text);
    }
}
