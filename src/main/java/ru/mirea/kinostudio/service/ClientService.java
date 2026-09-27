package ru.mirea.kinostudio.service;

import ru.mirea.kinostudio.exception.BusinessException;
import ru.mirea.kinostudio.exception.EntityNotFoundException;
import ru.mirea.kinostudio.model.Client;
import ru.mirea.kinostudio.repository.ClientRepository;

import java.sql.SQLException;
import java.util.List;

public class ClientService {

    private final ClientRepository repository;

    public ClientService(ClientRepository repository) {
        this.repository = repository;
    }

    public Client create(String fullName, String email, String phone) throws BusinessException, SQLException {
        // Бизнес-правило: обязательные поля не могут быть пустыми
        if (fullName == null || fullName.isBlank()) {
            throw new BusinessException("ФИО клиента не может быть пустым.");
        }
        if (email == null || email.isBlank() || !email.contains("@")) {
            throw new BusinessException("Некорректный email клиента.");
        }
        if (phone == null || phone.isBlank()) {
            throw new BusinessException("Телефон клиента не может быть пустым.");
        }
        return repository.save(new Client(fullName, email, phone));
    }

    public Client getById(int id) throws EntityNotFoundException, SQLException {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Клиент с ID=" + id + " не найден."));
    }

    public List<Client> getAll() throws SQLException {
        return repository.findAll();
    }

    public void update(int id, String fullName, String email, String phone)
            throws EntityNotFoundException, BusinessException, SQLException {
        Client c = getById(id);
        if (fullName == null || fullName.isBlank()) {
            throw new BusinessException("ФИО клиента не может быть пустым.");
        }
        c.setFullName(fullName);
        c.setEmail(email);
        c.setPhone(phone);
        repository.update(c);
    }

    public void delete(int id) throws EntityNotFoundException, SQLException {
        getById(id); // проверка существования
        repository.deleteById(id);
    }

    public boolean exists(int id) throws SQLException {
        return repository.existsById(id);
    }
}
