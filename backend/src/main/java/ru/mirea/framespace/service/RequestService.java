package ru.mirea.framespace.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.framespace.config.AppProperties;
import ru.mirea.framespace.domain.ShootingRequest;
import ru.mirea.framespace.domain.RequestStatus;
import ru.mirea.framespace.domain.Location;
import ru.mirea.framespace.domain.UserAccount;
import ru.mirea.framespace.dto.RequestDtos.BulkAction;
import ru.mirea.framespace.dto.RequestDtos.BulkFailure;
import ru.mirea.framespace.dto.RequestDtos.BulkRequest;
import ru.mirea.framespace.dto.RequestDtos.BulkResult;
import ru.mirea.framespace.dto.RequestDtos.RequestUpsert;
import ru.mirea.framespace.dto.RequestDtos.RequestResponse;
import ru.mirea.framespace.error.BadRequestException;
import ru.mirea.framespace.error.ConflictException;
import ru.mirea.framespace.error.NotFoundException;
import ru.mirea.framespace.filter.SmartFilterParser;
import ru.mirea.framespace.filter.RequestFilter;
import ru.mirea.framespace.filter.RequestSpecifications;
import ru.mirea.framespace.repository.ShootingRequestRepository;
import ru.mirea.framespace.repository.LocationRepository;
import ru.mirea.framespace.security.CurrentUserService;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Основной прикладной сервис заявок на съёмку. */
@Service
public class RequestService {
    private final ShootingRequestRepository requests;
    private final LocationRepository locations;
    private final CurrentUserService currentUser;
    private final SmartFilterParser parser;
    private final RequestMapper mapper;
    private final PriorityCalculator priorityCalculator;
    private final AuditService audit;
    private final AppProperties properties;

    public RequestService(ShootingRequestRepository requests, LocationRepository locations,
                          CurrentUserService currentUser, SmartFilterParser parser, RequestMapper mapper,
                          PriorityCalculator priorityCalculator, AuditService audit,
                          AppProperties properties) {
        this.requests = requests;
        this.locations = locations;
        this.currentUser = currentUser;
        this.parser = parser;
        this.mapper = mapper;
        this.priorityCalculator = priorityCalculator;
        this.audit = audit;
        this.properties = properties;
    }

    /** Выполняет безопасный умный поиск с пагинацией. */
    @Transactional(readOnly = true)
    public Page<RequestResponse> search(String query, Pageable pageable) {
        RequestFilter filter = parser.parse(query);
        return requests.findAll(RequestSpecifications.from(filter), pageable).map(mapper::toResponse);
    }

    /** Возвращает одну заявку независимо от архивного статуса. */
    @Transactional(readOnly = true)
    public RequestResponse get(UUID id) {
        return mapper.toResponse(require(id));
    }

    /** Создаёт заявку и рассчитывает приоритет на сервере. */
    @Transactional
    public RequestResponse create(RequestUpsert request) {
        UserAccount actor = currentUser.requireEntity();
        validateExternalId(request.externalId(), null);
        ShootingRequest entity = new ShootingRequest(UUID.randomUUID(), normalizeRequired(request.title()),
                normalizeRequired(request.clientName()), actor);
        apply(entity, request, actor);
        requests.save(entity);
        audit.record("REQUEST_CREATED", "REQUEST", entity.getId(),
                "Создана заявка " + displayName(entity), Map.of());
        return mapper.toResponse(entity);
    }

    /** Изменяет заявку только при совпадении optimistic version. */
    @Transactional
    public RequestResponse update(UUID id, RequestUpsert request) {
        ShootingRequest entity = require(id);
        verifyVersion(request.version(), entity.getVersion());
        validateExternalId(request.externalId(), id);
        UserAccount actor = currentUser.requireEntity();
        apply(entity, request, actor);
        audit.record("REQUEST_UPDATED", "REQUEST", id,
                "Изменена заявка " + displayName(entity), Map.of("version", entity.getVersion()));
        return mapper.toResponse(entity);
    }

    /** Архивирует заявку вместо физического удаления. */
    @Transactional
    public RequestResponse archive(UUID id) {
        ShootingRequest entity = require(id);
        archiveInternal(entity, currentUser.requireEntity());
        audit.record("REQUEST_ARCHIVED", "REQUEST", id,
                "Заявка перемещена в архив", Map.of());
        return mapper.toResponse(entity);
    }

    /** Возвращает заявку из архива. */
    @Transactional
    public RequestResponse restore(UUID id) {
        ShootingRequest entity = require(id);
        entity.setArchived(false);
        entity.setArchivedAt(null);
        entity.setArchivedBy(null);
        entity.setUpdatedBy(currentUser.requireEntity());
        audit.record("REQUEST_RESTORED", "REQUEST", id,
                "Заявка восстановлена из архива", Map.of());
        return mapper.toResponse(entity);
    }

    /**
     * Безвозвратно удаляет запись только при включённом feature flag и явном подтверждении.
     */
    @Transactional
    public void permanentDelete(UUID id, String confirmation) {
        if (!properties.permanentDeleteEnabled()) {
            throw new BadRequestException("Безвозвратное удаление отключено конфигурацией сервера");
        }
        if (!"PERMANENTLY_DELETE".equals(confirmation)) {
            throw new BadRequestException("Для удаления требуется заголовок X-Confirm-Delete: PERMANENTLY_DELETE");
        }
        ShootingRequest entity = require(id);
        String name = displayName(entity);
        requests.delete(entity);
        audit.record("REQUEST_PERMANENTLY_DELETED", "REQUEST", id,
                "Безвозвратно удалена заявка " + name, Map.of());
    }

    /** Выполняет ограниченную массовую операцию и сообщает ошибки отдельных UUID. */
    @Transactional
    public BulkResult bulk(BulkRequest request) {
        validateBulkRequest(request);
        UserAccount actor = currentUser.requireEntity();
        int succeeded = 0;
        var failures = new ArrayList<BulkFailure>();
        for (UUID id : request.ids()) {
            ShootingRequest entity = requests.findById(id).orElse(null);
            if (entity == null) {
                failures.add(new BulkFailure(id, "Заявка не найдена"));
                continue;
            }
            try {
                applyBulk(entity, request, actor);
                succeeded++;
            } catch (RuntimeException ex) {
                failures.add(new BulkFailure(id, ex.getMessage()));
            }
        }
        audit.record("REQUEST_BULK_" + request.action().name(), "REQUEST", null,
                "Выполнена массовая операция над заявками", Map.of("requested", request.ids().size(), "succeeded", succeeded));
        return new BulkResult(request.ids().size(), succeeded, failures);
    }

    /** Используется CSV-сервисом внутри транзакции без дублирования правил. */
    ShootingRequest upsertFromImport(ShootingRequest existing, RequestUpsert request, UserAccount actor) {
        if (existing == null) {
            ShootingRequest created = new ShootingRequest(UUID.randomUUID(), normalizeRequired(request.title()),
                    normalizeRequired(request.clientName()), actor);
            apply(created, request, actor);
            return requests.save(created);
        }
        apply(existing, request, actor);
        return existing;
    }

    private void apply(ShootingRequest entity, RequestUpsert request, UserAccount actor) {
        entity.setExternalId(normalizeNullable(request.externalId()));
        entity.setTitle(normalizeRequired(request.title()));
        entity.setDescription(normalizeRequired(request.description()));
        entity.setClientName(normalizeRequired(request.clientName()));
        entity.setClientEmail(normalizeEmail(request.clientEmail()));
        entity.setClientPhone(normalizeNullable(request.clientPhone()));
        entity.setShootingDate(request.shootingDate());
        entity.setLocation(resolveLocation(request.locationId()));
        entity.setStatus(request.status());
        entity.setBudget(request.budget());
        entity.setDurationHours(request.durationHours());
        entity.setUrgent(request.urgent());
        entity.setTags(normalizeTags(request.tags()));
        entity.setPriorityLevel(priorityCalculator.calculate(
                request.status(), request.budget(), request.durationHours(), request.urgent()));
        entity.setUpdatedBy(actor);
    }

    private void applyBulk(ShootingRequest entity, BulkRequest request, UserAccount actor) {
        switch (request.action()) {
            case ARCHIVE -> archiveInternal(entity, actor);
            case RESTORE -> {
                entity.setArchived(false);
                entity.setArchivedAt(null);
                entity.setArchivedBy(null);
                entity.setUpdatedBy(actor);
            }
            case SET_STATUS -> {
                entity.setStatus(request.status());
                entity.setPriorityLevel(priorityCalculator.calculate(
                        entity.getStatus(), entity.getBudget(), entity.getDurationHours(), entity.isUrgent()));
                entity.setUpdatedBy(actor);
            }
            case ADD_TAG -> {
                Set<String> tags = new LinkedHashSet<>(entity.getTags());
                tags.add(normalizeTag(request.tag()));
                entity.setTags(tags);
                entity.setUpdatedBy(actor);
            }
            case REMOVE_TAG -> {
                Set<String> tags = new LinkedHashSet<>(entity.getTags());
                tags.remove(normalizeTag(request.tag()));
                entity.setTags(tags);
                entity.setUpdatedBy(actor);
            }
        }
    }

    private static void validateBulkRequest(BulkRequest request) {
        if (request.action() == BulkAction.SET_STATUS && request.status() == null) {
            throw new BadRequestException("Для SET_STATUS требуется status");
        }
        if ((request.action() == BulkAction.ADD_TAG || request.action() == BulkAction.REMOVE_TAG)
                && (request.tag() == null || request.tag().isBlank())) {
            throw new BadRequestException("Для операции с тегом требуется tag");
        }
    }

    private static void archiveInternal(ShootingRequest entity, UserAccount actor) {
        entity.setArchived(true);
        entity.setArchivedAt(Instant.now());
        entity.setArchivedBy(actor);
        entity.setUpdatedBy(actor);
    }

    private ShootingRequest require(UUID id) {
        return requests.findById(id).orElseThrow(() -> new NotFoundException("Заявка не найдена"));
    }

    private Location resolveLocation(UUID id) {
        if (id == null) return null;
        Location location = locations.findById(id).orElseThrow(() -> new BadRequestException("Локация не найдена"));
        if (!location.isActive()) throw new BadRequestException("Нельзя назначить неактивную локацию");
        return location;
    }

    private void validateExternalId(String externalId, UUID currentId) {
        String normalized = normalizeNullable(externalId);
        if (normalized == null) return;
        boolean exists = currentId == null
                ? requests.existsByExternalIdIgnoreCase(normalized)
                : requests.existsByExternalIdIgnoreCaseAndIdNot(normalized, currentId);
        if (exists) throw new ConflictException("Внешний идентификатор уже используется");
    }

    private static void verifyVersion(Long requested, long actual) {
        if (requested == null) throw new BadRequestException("Для изменения требуется поле version");
        if (requested != actual) throw new ConflictException("Заявка уже изменена. Обновите данные и повторите операцию");
    }

    private static Set<String> normalizeTags(Set<String> tags) {
        if (tags == null) return Set.of();
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (String tag : tags) result.add(normalizeTag(tag));
        if (result.size() > 20) throw new BadRequestException("Допустимо не более 20 тегов");
        return result;
    }

    private static String normalizeTag(String value) {
        String normalized = normalizeRequired(value).toLowerCase(Locale.ROOT);
        if (normalized.length() > 50) throw new BadRequestException("Тег длиннее 50 символов");
        return normalized;
    }

    private static String normalizeEmail(String value) {
        String normalized = normalizeNullable(value);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private static String normalizeRequired(String value) {
        if (value == null || value.trim().isEmpty()) throw new BadRequestException("Обязательное текстовое поле не заполнено");
        return value.trim().replaceAll("\\s+", " ");
    }

    private static String normalizeNullable(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim().replaceAll("\\s+", " ");
    }

    private static String displayName(ShootingRequest entity) {
        return entity.getTitle();
    }
}
