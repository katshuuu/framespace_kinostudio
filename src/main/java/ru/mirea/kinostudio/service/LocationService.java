package ru.mirea.kinostudio.service;

import ru.mirea.kinostudio.exception.BusinessException;
import ru.mirea.kinostudio.exception.EntityNotFoundException;
import ru.mirea.kinostudio.model.Location;
import ru.mirea.kinostudio.model.LocationType;
import ru.mirea.kinostudio.repository.LocationRepository;

import java.sql.SQLException;
import java.util.List;

public class LocationService {

    private final LocationRepository repository;

    public LocationService(LocationRepository repository) {
        this.repository = repository;
    }

    public Location create(String name, String address, int capacity, LocationType type)
            throws BusinessException, SQLException {
        if (name == null || name.isBlank()) {
            throw new BusinessException("Название локации не может быть пустым.");
        }
        if (address == null || address.isBlank()) {
            throw new BusinessException("Адрес локации не может быть пустым.");
        }
        if (capacity <= 0) {
            throw new BusinessException("Вместимость локации должна быть больше нуля.");
        }
        return repository.save(new Location(name, address, capacity, type));
    }

    public Location getById(int id) throws EntityNotFoundException, SQLException {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Локация с ID=" + id + " не найдена."));
    }

    public List<Location> getAll() throws SQLException {
        return repository.findAll();
    }

    public void update(int id, String name, String address, int capacity, LocationType type)
            throws EntityNotFoundException, BusinessException, SQLException {
        Location loc = getById(id);
        if (name == null || name.isBlank()) {
            throw new BusinessException("Название локации не может быть пустым.");
        }
        if (capacity <= 0) {
            throw new BusinessException("Вместимость локации должна быть больше нуля.");
        }
        loc.setName(name);
        loc.setAddress(address);
        loc.setCapacity(capacity);
        loc.setType(type);
        repository.update(loc);
    }

    public void delete(int id) throws EntityNotFoundException, BusinessException, SQLException {
        getById(id);
        // Бизнес-правило: нельзя удалить локацию, на которую есть активные заявки
        if (repository.hasActiveRequests(id)) {
            throw new BusinessException("Нельзя удалить локацию: на неё есть активные заявки на съёмку.");
        }
        repository.deleteById(id);
    }

    public boolean exists(int id) throws SQLException {
        return repository.existsById(id);
    }
}
