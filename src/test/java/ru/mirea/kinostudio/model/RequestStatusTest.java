package ru.mirea.kinostudio.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RequestStatusTest {

    @Test
    @DisplayName("разбор RequestStatus.fromString")
    void fromStringParsesKnownStatusAndRejectsUnknown() {
        assertEquals(RequestStatus.APPROVED, RequestStatus.fromString("approved"));
        assertEquals(RequestStatus.IN_PROGRESS, RequestStatus.fromString(" IN_PROGRESS "));
        assertThrows(IllegalArgumentException.class, () -> RequestStatus.fromString("DONE"));
        assertThrows(IllegalArgumentException.class, () -> RequestStatus.fromString(null));
    }
}
