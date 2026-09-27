package ru.mirea.kinostudio.model;

/**
 * Тип локации киностудии.
 */
public enum LocationType {
    PAVILION,  // съёмочный павильон
    OUTDOOR,   // натурная площадка
    STUDIO,    // небольшая студия
    OFFICE;    // офисное/переговорное помещение

    public static LocationType fromString(String value) {
        try {
            return LocationType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("Некорректный тип локации: " + value);
        }
    }
}
