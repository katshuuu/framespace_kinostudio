package ru.mirea.kinostudio.util;

import ru.mirea.kinostudio.model.LocationType;
import ru.mirea.kinostudio.model.RequestStatus;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Человекочитаемые подписи для интерфейса: в базе хранятся коды (NEW, PAVILION...),
 * а пользователь должен видеть русские названия, суммы и даты в привычном формате.
 */
public final class Labels {

    public static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    public static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final Locale RU = Locale.forLanguageTag("ru-RU");

    private Labels() {
    }

    public static String status(RequestStatus s) {
        if (s == null) return "—";
        return switch (s) {
            case NEW -> "Новая";
            case APPROVED -> "Согласована";
            case IN_PROGRESS -> "В работе";
            case COMPLETED -> "Завершена";
            case REJECTED -> "Отклонена";
            case CANCELLED -> "Отменена";
        };
    }

    public static String type(LocationType t) {
        if (t == null) return "—";
        return switch (t) {
            case PAVILION -> "Павильон";
            case OUTDOOR -> "Натурная площадка";
            case STUDIO -> "Студия";
            case OFFICE -> "Офис";
        };
    }

    public static String money(BigDecimal value) {
        if (value == null) return "—";
        NumberFormat nf = NumberFormat.getNumberInstance(RU);
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        return nf.format(value) + " ₽";
    }

    public static String number(double value, int fractionDigits) {
        NumberFormat nf = NumberFormat.getNumberInstance(RU);
        nf.setMinimumFractionDigits(fractionDigits);
        nf.setMaximumFractionDigits(fractionDigits);
        return nf.format(value);
    }

    public static String date(LocalDate d) {
        return d == null ? "—" : DATE.format(d);
    }

    public static String dateTime(LocalDateTime d) {
        return d == null ? "—" : DATE_TIME.format(d);
    }
}
