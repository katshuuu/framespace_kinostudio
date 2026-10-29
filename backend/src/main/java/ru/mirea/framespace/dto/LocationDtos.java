package ru.mirea.framespace.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import ru.mirea.framespace.domain.LocationType;

import java.time.Instant;
import java.util.UUID;

/** DTO-контракты локаций съёмки. */
public final class LocationDtos {
    private LocationDtos() {
    }

    public record LocationUpsert(
            @NotBlank @Size(max = 50) String code,
            @Size(max = 200) String name,
            @Size(max = 500) String address,
            @Min(1) Integer capacity,
            LocationType type,
            Boolean active,
            Long version) {
    }

    public record LocationResponse(
            UUID id,
            String code,
            String name,
            String address,
            Integer capacity,
            LocationType type,
            boolean active,
            Instant createdAt,
            Instant updatedAt,
            long version) {
    }
}
