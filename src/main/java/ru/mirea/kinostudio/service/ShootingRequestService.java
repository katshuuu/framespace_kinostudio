package ru.mirea.kinostudio.service;

import ru.mirea.kinostudio.exception.BusinessException;
import ru.mirea.kinostudio.exception.EntityNotFoundException;
import ru.mirea.kinostudio.model.RequestStatus;
import ru.mirea.kinostudio.model.ShootingRequest;
import ru.mirea.kinostudio.repository.ClientRepository;
import ru.mirea.kinostudio.repository.ShootingRequestRepository;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

public class ShootingRequestService {

    private final ShootingRequestRepository repository;
    private final ClientRepository clientRepository;
    private final ru.mirea.kinostudio.repository.LocationRepository locationRepository;

    // Разрешённый граф переходов статусов (бизнес-правило №6)
    private static final Map<RequestStatus, EnumSet<RequestStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(RequestStatus.class);
    static {
        ALLOWED_TRANSITIONS.put(RequestStatus.NEW, EnumSet.of(RequestStatus.APPROVED, RequestStatus.REJECTED));
        ALLOWED_TRANSITIONS.put(RequestStatus.APPROVED, EnumSet.of(RequestStatus.IN_PROGRESS, RequestStatus.CANCELLED));
        ALLOWED_TRANSITIONS.put(RequestStatus.IN_PROGRESS, EnumSet.of(RequestStatus.COMPLETED, RequestStatus.CANCELLED));
        ALLOWED_TRANSITIONS.put(RequestStatus.COMPLETED, EnumSet.noneOf(RequestStatus.class));
        ALLOWED_TRANSITIONS.put(RequestStatus.REJECTED, EnumSet.noneOf(RequestStatus.class));
        ALLOWED_TRANSITIONS.put(RequestStatus.CANCELLED, EnumSet.noneOf(RequestStatus.class));
    }

    public ShootingRequestService(ShootingRequestRepository repository, ClientRepository clientRepository,
                                   ru.mirea.kinostudio.repository.LocationRepository locationRepository) {
        this.repository = repository;
        this.clientRepository = clientRepository;
        this.locationRepository = locationRepository;
    }

    public ShootingRequest create(int clientId, int locationId, String title, String description,
                                   LocalDate shootingDate, int durationHours, BigDecimal budget)
            throws BusinessException, SQLException {

        validateCommon(clientId, locationId, title, description, shootingDate, durationHours, budget);

        ShootingRequest r = new ShootingRequest(clientId, locationId, title, description,
                shootingDate, durationHours, budget);
        return repository.save(r);
    }

    public ShootingRequest getById(int id) throws EntityNotFoundException, SQLException {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Заявка с ID=" + id + " не найдена."));
    }

    public List<ShootingRequest> getAll() throws SQLException {
        return repository.findAll();
    }

    public void update(int id, int clientId, int locationId, String title, String description,
                        LocalDate shootingDate, int durationHours, BigDecimal budget)
            throws EntityNotFoundException, BusinessException, SQLException {

        ShootingRequest r = getById(id);
        validateCommon(clientId, locationId, title, description, shootingDate, durationHours, budget);

        r.setClientId(clientId);
        r.setLocationId(locationId);
        r.setTitle(title);
        r.setDescription(description);
        r.setShootingDate(shootingDate);
        r.setDurationHours(durationHours);
        r.setBudget(budget);
        repository.update(r);
    }

    public void changeStatus(int id, RequestStatus newStatus)
            throws EntityNotFoundException, BusinessException, SQLException {
        ShootingRequest r = getById(id);
        EnumSet<RequestStatus> allowed = ALLOWED_TRANSITIONS.get(r.getStatus());
        if (!allowed.contains(newStatus)) {
            throw new BusinessException("Недопустимый переход статуса: " + r.getStatus() + " -> " + newStatus);
        }
        r.setStatus(newStatus);
        repository.update(r);
    }

    public void delete(int id) throws EntityNotFoundException, SQLException {
        getById(id);
        repository.deleteById(id);
    }

    /** Статусы, в которые можно перевести заявку из текущего (нужно интерфейсу для выбора). */
    public java.util.Set<RequestStatus> getAllowedTransitions(RequestStatus from) {
        return java.util.Collections.unmodifiableSet(ALLOWED_TRANSITIONS.get(from));
    }

    // ---- Поиск / фильтрация / сортировка (делегирование в репозиторий) ----

    public List<ShootingRequest> searchByTitle(String keyword) throws SQLException {
        return repository.searchByTitle(keyword);
    }

    public List<ShootingRequest> searchByLocation(String keyword) throws SQLException {
        return repository.searchByLocationName(keyword);
    }

    public List<ShootingRequest> filterByStatus(RequestStatus status) throws SQLException {
        return repository.filterByStatus(status);
    }

    public List<ShootingRequest> filterByDateRange(LocalDate from, LocalDate to) throws BusinessException, SQLException {
        if (from.isAfter(to)) {
            throw new BusinessException("Дата начала диапазона не может быть позже даты окончания.");
        }
        return repository.filterByDateRange(from, to);
    }

    public List<ShootingRequest> sortByDate() throws SQLException {
        return repository.findAllSortedByDate();
    }

    public List<ShootingRequest> sortByBudget() throws SQLException {
        return repository.findAllSortedByBudget();
    }

    // ---- Статистика ----

    public Map<String, Object> getStatistics() throws SQLException {
        Map<String, Object> stats = new java.util.LinkedHashMap<>();
        stats.put("Всего заявок", repository.countAll());
        for (RequestStatus status : RequestStatus.values()) {
            stats.put("  " + status.name(), repository.countByStatus(status));
        }
        stats.put("Суммарный бюджет всех заявок", repository.sumBudget());
        stats.put("Средняя длительность съёмки (ч)", String.format("%.1f", repository.avgDuration()));
        return stats;
    }

    // ---- Валидация бизнес-правил ----

    private void validateCommon(int clientId, int locationId, String title, String description,
                                 LocalDate shootingDate, int durationHours, BigDecimal budget)
            throws BusinessException, SQLException {

        // Правило 1: название и описание обязательны
        if (title == null || title.isBlank()) {
            throw new BusinessException("Название заявки не может быть пустым.");
        }
        if (description == null || description.isBlank()) {
            throw new BusinessException("Описание заявки не может быть пустым.");
        }

        // Правило 2: клиент должен существовать (проверка внешнего ключа на уровне логики)
        if (!clientRepository.existsById(clientId)) {
            throw new BusinessException("Клиент с ID=" + clientId + " не существует.");
        }

        // Правило 7: локация должна существовать (проверка второго внешнего ключа)
        if (!locationRepository.existsById(locationId)) {
            throw new BusinessException("Локация с ID=" + locationId + " не существует.");
        }

        // Правило 3: дата съёмки не может быть в прошлом
        if (shootingDate == null || shootingDate.isBefore(LocalDate.now())) {
            throw new BusinessException("Дата съёмки не может быть в прошлом.");
        }

        // Правило 4: длительность и бюджет должны быть корректными
        if (durationHours <= 0) {
            throw new BusinessException("Длительность съёмки должна быть больше нуля.");
        }
        if (budget == null || budget.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Бюджет не может быть отрицательным.");
        }
    }
}
