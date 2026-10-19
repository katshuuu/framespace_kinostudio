package ru.mirea.kinostudio.service;

import ru.mirea.kinostudio.model.Client;
import ru.mirea.kinostudio.model.Location;
import ru.mirea.kinostudio.model.RequestView;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Собирает заявки вместе с клиентами и локациями (для отображения в таблице).
 * Работает поверх существующих сервисов КР №1 — SQL здесь нет.
 */
public class RequestViewService {

    private final ShootingRequestService requestService;
    private final ClientService clientService;
    private final LocationService locationService;

    public RequestViewService(ShootingRequestService requestService,
                              ClientService clientService,
                              LocationService locationService) {
        this.requestService = requestService;
        this.clientService = clientService;
        this.locationService = locationService;
    }

    public List<RequestView> loadAll() throws SQLException {
        Map<Integer, Client> clients = clientService.getAll().stream()
                .collect(Collectors.toMap(Client::getId, Function.identity()));
        Map<Integer, Location> locations = locationService.getAll().stream()
                .collect(Collectors.toMap(Location::getId, Function.identity()));
        return requestService.getAll().stream()
                .map(r -> new RequestView(r, clients.get(r.getClientId()), locations.get(r.getLocationId())))
                .toList();
    }
}
