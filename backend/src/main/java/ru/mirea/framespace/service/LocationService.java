package ru.mirea.framespace.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.framespace.domain.Location;
import ru.mirea.framespace.dto.LocationDtos.LocationUpsert;
import ru.mirea.framespace.dto.LocationDtos.LocationResponse;
import ru.mirea.framespace.error.BadRequestException;
import ru.mirea.framespace.error.ConflictException;
import ru.mirea.framespace.error.NotFoundException;
import ru.mirea.framespace.repository.LocationRepository;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Прикладные операции над локациями съёмки. */
@Service
public class LocationService {
    private final LocationRepository locations;
    private final AuditService audit;

    public LocationService(LocationRepository locations, AuditService audit) {
        this.locations = locations;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<LocationResponse> list(boolean activeOnly) {
        return (activeOnly ? locations.findByActiveTrueOrderByCodeAsc() : locations.findAllByOrderByCodeAsc())
                .stream().map(LocationService::toResponse).toList();
    }

    @Transactional
    public LocationResponse create(LocationUpsert request) {
        String code = normalizeCode(request.code());
        if (locations.existsByCodeIgnoreCase(code)) throw new ConflictException("Локация с таким кодом уже существует");
        Location location = new Location(UUID.randomUUID(), code, normalizeNullable(request.name()),
                normalizeNullable(request.address()), request.capacity());
        if (request.type() != null) location.setType(request.type());
        if (request.active() != null) location.setActive(request.active());
        locations.saveAndFlush(location);
        audit.record("LOCATION_CREATED", "LOCATION", location.getId(), "Создана локация " + code, Map.of());
        return toResponse(location);
    }

    @Transactional
    public LocationResponse update(UUID id, LocationUpsert request) {
        Location location = require(id);
        if (request.version() == null || request.version() != location.getVersion())
            throw new ConflictException("Локация уже изменена. Обновите список");
        String code = normalizeCode(request.code());
        locations.findByCodeIgnoreCase(code).filter(other -> !other.getId().equals(id)).ifPresent(other -> {
            throw new ConflictException("Локация с таким кодом уже существует");
        });
        location.setCode(code);
        location.setName(normalizeNullable(request.name()));
        location.setAddress(normalizeNullable(request.address()));
        location.setCapacity(request.capacity());
        if (request.type() != null) location.setType(request.type());
        if (request.active() != null) location.setActive(request.active());
        audit.record("LOCATION_UPDATED", "LOCATION", id, "Изменена локация " + code, Map.of());
        locations.flush();
        return toResponse(location);
    }

    @Transactional
    public LocationResponse setActive(UUID id, boolean active, long version) {
        Location location = require(id);
        if (version != location.getVersion()) throw new ConflictException("Локация уже изменена. Обновите список");
        location.setActive(active);
        audit.record(active ? "LOCATION_ACTIVATED" : "LOCATION_DEACTIVATED", "LOCATION", id,
                (active ? "Активирована" : "Деактивирована") + " локация " + location.getCode(), Map.of());
        locations.flush();
        return toResponse(location);
    }

    private Location require(UUID id) {
        return locations.findById(id).orElseThrow(() -> new NotFoundException("Локация не найдена"));
    }

    private static LocationResponse toResponse(Location location) {
        return new LocationResponse(location.getId(), location.getCode(), location.getName(), location.getAddress(),
                location.getCapacity(), location.getType(), location.isActive(), location.getCreatedAt(), location.getUpdatedAt(),
                location.getVersion());
    }

    private static String normalizeCode(String value) {
        if (value == null || value.isBlank()) throw new BadRequestException("Код локации обязателен");
        return value.trim().replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
    }

    private static String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim().replaceAll("\\s+", " ");
    }
}
