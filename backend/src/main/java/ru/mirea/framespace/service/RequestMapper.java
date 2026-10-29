package ru.mirea.framespace.service;

import org.springframework.stereotype.Component;
import ru.mirea.framespace.domain.ShootingRequest;
import ru.mirea.framespace.dto.RequestDtos.RequestResponse;

import java.util.Set;

/** Преобразует entity в публичный DTO внутри активной транзакции. */
@Component
public class RequestMapper {
    public RequestResponse toResponse(ShootingRequest request) {
        return new RequestResponse(
                request.getId(),
                request.getExternalId(),
                request.getTitle(),
                request.getDescription(),
                request.getClient().getId(),
                request.getClient().getFullName(),
                request.getClient().getEmail(),
                request.getClient().getPhone(),
                request.getShootingDate(),
                request.getLocation() == null ? null : request.getLocation().getId(),
                request.getLocation() == null ? null : request.getLocation().getCode(),
                request.getStatus(),
                request.getStatus().nextStatuses(),
                request.getBudget(),
                request.getDurationHours(),
                request.isUrgent(),
                request.getPriorityLevel(),
                request.isArchived(),
                request.getArchivedAt(),
                Set.copyOf(request.getTags()),
                request.getCreatedAt(),
                request.getUpdatedAt(),
                request.getVersion());
    }
}
