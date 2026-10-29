package ru.mirea.framespace.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.framespace.domain.Client;
import ru.mirea.framespace.dto.ClientDtos.ClientResponse;
import ru.mirea.framespace.dto.ClientDtos.ClientUpsert;
import ru.mirea.framespace.error.BadRequestException;
import ru.mirea.framespace.error.ConflictException;
import ru.mirea.framespace.error.NotFoundException;
import ru.mirea.framespace.repository.ClientRepository;
import ru.mirea.framespace.repository.ShootingRequestRepository;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Прикладные операции над клиентами — заказчиками съёмки. */
@Service
public class ClientService {
    private final ClientRepository clients;
    private final ShootingRequestRepository requests;
    private final AuditService audit;

    public ClientService(ClientRepository clients, ShootingRequestRepository requests, AuditService audit) {
        this.clients = clients;
        this.requests = requests;
        this.audit = audit;
    }

    /** Поиск по ФИО/названию, email и телефону с пагинацией. */
    @Transactional(readOnly = true)
    public Page<ClientResponse> search(String query, Pageable pageable) {
        String text = query == null ? "" : query.trim();
        if (text.length() > 200) throw new BadRequestException("Строка поиска слишком длинная");
        Page<Client> page = clients.search("%" + escapeLike(text) + "%", pageable);
        Map<UUID, Long> counts = new HashMap<>();
        if (!page.isEmpty()) {
            for (Object[] row : requests.countByClientIds(page.map(Client::getId).getContent())) {
                counts.put((UUID) row[0], (Long) row[1]);
            }
        }
        return page.map(client -> toResponse(client, counts.getOrDefault(client.getId(), 0L)));
    }

    @Transactional(readOnly = true)
    public ClientResponse get(UUID id) {
        Client client = require(id);
        return toResponse(client, requests.countByClient_Id(id));
    }

    @Transactional
    public ClientResponse create(ClientUpsert request) {
        Client client = createInternal(request);
        return toResponse(client, 0);
    }

    @Transactional
    public ClientResponse update(UUID id, ClientUpsert request) {
        Client client = require(id);
        if (request.version() == null) throw new BadRequestException("Для изменения требуется поле version");
        if (request.version() != client.getVersion()) {
            throw new ConflictException("Клиент уже изменён. Обновите данные и повторите операцию");
        }
        String email = normalizeEmail(request.email());
        clients.findByEmailIgnoreCase(email).filter(other -> !other.getId().equals(id)).ifPresent(other -> {
            throw new ConflictException("Клиент с таким email уже существует");
        });
        client.setFullName(normalizeRequired(request.fullName()));
        client.setEmail(email);
        client.setPhone(request.phone().trim());
        audit.record("CLIENT_UPDATED", "CLIENT", id, "Изменён клиент " + client.getFullName(), Map.of());
        clients.flush();
        return toResponse(client, requests.countByClient_Id(id));
    }

    /** Удаление разрешено только клиенту без заявок, включая архивные. */
    @Transactional
    public void delete(UUID id) {
        Client client = require(id);
        long linked = requests.countByClient_Id(id);
        if (linked > 0) {
            throw new ConflictException("Нельзя удалить клиента: с ним связано заявок — " + linked);
        }
        String name = client.getFullName();
        clients.delete(client);
        audit.record("CLIENT_DELETED", "CLIENT", id, "Удалён клиент " + name, Map.of());
    }

    /** Возвращает клиента для заявки; используется RequestService. */
    Client requireForRequest(UUID id) {
        if (id == null) throw new BadRequestException("Для заявки требуется клиент");
        return clients.findById(id).orElseThrow(() -> new BadRequestException("Клиент не найден"));
    }

    /**
     * CSV-импорт: существующий клиент находится по email, новый создаётся из строки файла.
     */
    Client findOrCreateForImport(ClientUpsert request) {
        return clients.findByEmailIgnoreCase(normalizeEmail(request.email()))
                .orElseGet(() -> createInternal(request));
    }

    private Client createInternal(ClientUpsert request) {
        String email = normalizeEmail(request.email());
        if (clients.existsByEmailIgnoreCase(email)) throw new ConflictException("Клиент с таким email уже существует");
        Client client = new Client(UUID.randomUUID(), normalizeRequired(request.fullName()), email, request.phone().trim());
        clients.saveAndFlush(client);
        audit.record("CLIENT_CREATED", "CLIENT", client.getId(), "Создан клиент " + client.getFullName(), Map.of());
        return client;
    }

    private Client require(UUID id) {
        return clients.findById(id).orElseThrow(() -> new NotFoundException("Клиент не найден"));
    }

    private static ClientResponse toResponse(Client client, long requestCount) {
        return new ClientResponse(client.getId(), client.getFullName(), client.getEmail(), client.getPhone(),
                requestCount, client.getCreatedAt(), client.getUpdatedAt(), client.getVersion());
    }

    static String normalizeEmail(String value) {
        return normalizeRequired(value).toLowerCase(Locale.ROOT);
    }

    private static String normalizeRequired(String value) {
        if (value == null || value.trim().isEmpty()) throw new BadRequestException("Обязательное текстовое поле не заполнено");
        return value.trim().replaceAll("\\s+", " ");
    }

    private static String escapeLike(String value) {
        return value.toLowerCase(Locale.ROOT).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
