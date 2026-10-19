package ru.mirea.kinostudio.fx;

import javafx.util.StringConverter;
import ru.mirea.kinostudio.model.Client;
import ru.mirea.kinostudio.model.Location;
import ru.mirea.kinostudio.model.LocationType;
import ru.mirea.kinostudio.model.RequestStatus;
import ru.mirea.kinostudio.util.Labels;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.function.Function;

/**
 * Конвертеры «объект → текст» для ComboBox и DatePicker:
 * в списке показываем русские названия, а не коды enum и не toString() объекта.
 */
public final class Converters {

    private Converters() {
    }

    public static <T> StringConverter<T> of(Function<T, String> toText) {
        return new StringConverter<>() {
            @Override
            public String toString(T value) {
                return value == null ? "" : toText.apply(value);
            }

            @Override
            public T fromString(String text) {
                return null; // выбор только из списка
            }
        };
    }

    public static StringConverter<RequestStatus> status() {
        return of(Labels::status);
    }

    public static StringConverter<LocationType> locationType() {
        return of(Labels::type);
    }

    public static StringConverter<Location> location() {
        return of(l -> l.getName() + " (" + Labels.type(l.getType()) + ")");
    }

    public static StringConverter<Client> client() {
        return of(c -> c.getFullName() + " · " + c.getEmail());
    }

    /** Дата в формате дд.мм.гггг; при неверном вводе возвращает null (проверка — в форме). */
    public static StringConverter<LocalDate> date() {
        return new StringConverter<>() {
            @Override
            public String toString(LocalDate d) {
                return d == null ? "" : Labels.DATE.format(d);
            }

            @Override
            public LocalDate fromString(String text) {
                if (text == null || text.isBlank()) return null;
                try {
                    return LocalDate.parse(text.trim(), Labels.DATE);
                } catch (DateTimeParseException e) {
                    return null;
                }
            }
        };
    }
}
