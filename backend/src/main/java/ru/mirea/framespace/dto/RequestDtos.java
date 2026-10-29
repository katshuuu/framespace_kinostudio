package ru.mirea.framespace.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import ru.mirea.framespace.domain.PriorityLevel;
import ru.mirea.framespace.domain.RequestStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** DTO-контракты заявок на съёмку и массовых операций. */
public final class RequestDtos {
    private RequestDtos() {
    }

    /** Входная модель создания и изменения заявки. */
    public record RequestUpsert(
            @Size(max = 100) String externalId,
            @NotBlank @Size(max = 200) String title,
            @NotBlank @Size(max = 4000) String description,
            @NotBlank @Size(max = 200) String clientName,
            @Email @Size(max = 254) String clientEmail,
            @Pattern(regexp = "^\\+7-\\d{3}-\\d{3}-\\d{2}-\\d{2}$", message = "некорректный телефон") String clientPhone,
            @NotNull LocalDate shootingDate,
            UUID locationId,
            @NotNull RequestStatus status,
            @DecimalMin("0.00") @DecimalMax("99999999.99") BigDecimal budget,
            @Min(1) Integer durationHours,
            boolean urgent,
            @Size(max = 20) Set<@NotBlank @Size(max = 50) String> tags,
            Long version) {
    }

    /** Выходная карточка заявки. */
    public record RequestResponse(
            UUID id,
            String externalId,
            String title,
            String description,
            String clientName,
            String clientEmail,
            String clientPhone,
            LocalDate shootingDate,
            UUID locationId,
            String locationCode,
            RequestStatus status,
            BigDecimal budget,
            Integer durationHours,
            boolean urgent,
            PriorityLevel priorityLevel,
            boolean archived,
            Instant archivedAt,
            Set<String> tags,
            Instant createdAt,
            Instant updatedAt,
            long version) {
    }

    /** Поддерживаемые массовые действия. */
    public enum BulkAction { ARCHIVE, RESTORE, SET_STATUS, ADD_TAG, REMOVE_TAG }

    /** Запрос массового изменения. */
    public record BulkRequest(
            @NotNull @Size(min = 1, max = 500) Set<UUID> ids,
            @NotNull BulkAction action,
            RequestStatus status,
            @Size(max = 50) String tag) {
    }

    /** Результат массовой операции с явным списком ошибок. */
    public record BulkResult(int requested, int succeeded, List<BulkFailure> failures) {
    }

    /** Ошибка отдельной записи в массовой операции. */
    public record BulkFailure(UUID id, String message) {
    }
}
