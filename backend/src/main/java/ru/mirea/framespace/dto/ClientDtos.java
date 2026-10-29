package ru.mirea.framespace.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

/** DTO-контракты клиентов (заказчиков съёмки). */
public final class ClientDtos {
    public static final String PHONE_PATTERN = "^\\+7-\\d{3}-\\d{3}-\\d{2}-\\d{2}$";

    private ClientDtos() {
    }

    public record ClientUpsert(
            @NotBlank @Size(max = 150) String fullName,
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Pattern(regexp = PHONE_PATTERN, message = "телефон в формате +7-XXX-XXX-XX-XX") String phone,
            Long version) {
    }

    public record ClientResponse(
            UUID id,
            String fullName,
            String email,
            String phone,
            long requestCount,
            Instant createdAt,
            Instant updatedAt,
            long version) {
    }
}
