package ru.mirea.kinostudio.api;

import ru.mirea.kinostudio.exception.BusinessException;
import ru.mirea.kinostudio.model.LocationType;
import ru.mirea.kinostudio.model.RequestStatus;
import ru.mirea.kinostudio.model.ShootingRequest;
import ru.mirea.kinostudio.service.ClientService;
import ru.mirea.kinostudio.service.LocationService;
import ru.mirea.kinostudio.service.ShootingRequestService;

import com.sun.net.httpserver.HttpExchange;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * CRUD клиентов, локаций и заявок. Правила не копируются: методы вызывают уже написанные сервисы.
 */
final class StudioResources {

    private final ClientService clients;
    private final LocationService locations;
    private final ShootingRequestService requests;

    StudioResources(ClientService clients, LocationService locations, ShootingRequestService requests) {
        this.clients = clients;
        this.locations = locations;
        this.requests = requests;
    }

    void handle(HttpExchange exchange, String method, String path) throws Exception {
        if (path.startsWith("/api/clients")) {
            clients(exchange, method, path);
            return;
        }
        if (path.startsWith("/api/locations")) {
            locations(exchange, method, path);
            return;
        }
        if (path.startsWith("/api/shooting-requests")) {
            requests(exchange, method, path);
            return;
        }
        if ("/api/statistics".equals(path)) {
            statistics(exchange, method);
            return;
        }
        ApiResponses.json(exchange, 404, Map.of("error", "Маршрут не найден."));
    }

    private void clients(HttpExchange exchange, String method, String path) throws Exception {
        if ("/api/clients".equals(path)) {
            if ("GET".equals(method)) {
                ApiResponses.json(exchange, 200, clients.getAll());
                return;
            }
            if ("POST".equals(method)) {
                ClientBody body = ApiJson.read(exchange, ClientBody.class);
                ApiResponses.json(exchange, 201, clients.create(body.fullName(), body.email(), body.phone()));
                return;
            }
            ApiResponses.methodNotAllowed(exchange);
            return;
        }
        int id = ApiResponses.id(path, "/api/clients/");
        if ("GET".equals(method)) {
            ApiResponses.json(exchange, 200, clients.getById(id));
            return;
        }
        if ("PUT".equals(method)) {
            ClientBody body = ApiJson.read(exchange, ClientBody.class);
            clients.update(id, body.fullName(), body.email(), body.phone());
            ApiResponses.json(exchange, 200, clients.getById(id));
            return;
        }
        if ("DELETE".equals(method)) {
            clients.delete(id);
            ApiResponses.noContent(exchange);
            return;
        }
        ApiResponses.methodNotAllowed(exchange);
    }

    private void locations(HttpExchange exchange, String method, String path) throws Exception {
        if ("/api/locations".equals(path)) {
            if ("GET".equals(method)) {
                ApiResponses.json(exchange, 200, locations.getAll());
                return;
            }
            if ("POST".equals(method)) {
                LocationBody body = ApiJson.read(exchange, LocationBody.class);
                ApiResponses.json(exchange, 201, locations.create(
                        body.name(), body.address(), capacity(body), locationType(body)));
                return;
            }
            ApiResponses.methodNotAllowed(exchange);
            return;
        }
        int id = ApiResponses.id(path, "/api/locations/");
        if ("GET".equals(method)) {
            ApiResponses.json(exchange, 200, locations.getById(id));
            return;
        }
        if ("PUT".equals(method)) {
            LocationBody body = ApiJson.read(exchange, LocationBody.class);
            locations.update(id, body.name(), body.address(), capacity(body), locationType(body));
            ApiResponses.json(exchange, 200, locations.getById(id));
            return;
        }
        if ("DELETE".equals(method)) {
            locations.delete(id);
            ApiResponses.noContent(exchange);
            return;
        }
        ApiResponses.methodNotAllowed(exchange);
    }

    private void requests(HttpExchange exchange, String method, String path) throws Exception {
        if ("/api/shooting-requests".equals(path)) {
            if ("GET".equals(method)) {
                ApiResponses.json(exchange, 200, list(exchange));
                return;
            }
            if ("POST".equals(method)) {
                createRequest(exchange);
                return;
            }
            ApiResponses.methodNotAllowed(exchange);
            return;
        }
        if (path.endsWith("/status")) {
            String base = path.substring(0, path.length() - "/status".length());
            int id = ApiResponses.id(base, "/api/shooting-requests/");
            if (!"PATCH".equals(method)) {
                ApiResponses.methodNotAllowed(exchange);
                return;
            }
            StatusBody body = ApiJson.read(exchange, StatusBody.class);
            requests.changeStatus(id, RequestStatus.fromString(body.status()));
            ApiResponses.json(exchange, 200, requests.getById(id));
            return;
        }
        int id = ApiResponses.id(path, "/api/shooting-requests/");
        if ("GET".equals(method)) {
            ApiResponses.json(exchange, 200, requests.getById(id));
            return;
        }
        if ("PUT".equals(method)) {
            ShootingRequestBody body = ApiJson.read(exchange, ShootingRequestBody.class);
            requireRequest(body);
            requests.update(id, body.clientId(), body.locationId(), body.title(), body.description(),
                    LocalDate.parse(body.shootingDate()), body.durationHours(), body.budget());
            ApiResponses.json(exchange, 200, requests.getById(id));
            return;
        }
        if ("DELETE".equals(method)) {
            requests.delete(id);
            ApiResponses.noContent(exchange);
            return;
        }
        ApiResponses.methodNotAllowed(exchange);
    }

    private void createRequest(HttpExchange exchange) throws Exception {
        ShootingRequestBody body = ApiJson.read(exchange, ShootingRequestBody.class);
        requireRequest(body);
        ShootingRequest created = requests.create(
                body.clientId(), body.locationId(),
                body.title(), body.description(),
                LocalDate.parse(body.shootingDate()),
                body.durationHours(), body.budget());
        ApiResponses.json(exchange, 201, created);
    }

    private List<ShootingRequest> list(HttpExchange exchange) throws Exception {
        Map<String, String> query = ApiResponses.query(exchange);
        String title = ApiResponses.blankToNull(query.get("title"));
        String location = ApiResponses.blankToNull(query.get("location"));
        String status = ApiResponses.blankToNull(query.get("status"));
        String from = ApiResponses.blankToNull(query.get("from"));
        String to = ApiResponses.blankToNull(query.get("to"));
        String sort = ApiResponses.blankToNull(query.get("sort"));
        if (title != null) {
            return requests.searchByTitle(title);
        }
        if (location != null) {
            return requests.searchByLocation(location);
        }
        if (status != null) {
            return requests.filterByStatus(RequestStatus.fromString(status));
        }
        if (from != null || to != null) {
            if (from == null || to == null) {
                throw new BusinessException("Для фильтра по датам нужны оба параметра: from и to.");
            }
            return requests.filterByDateRange(LocalDate.parse(from), LocalDate.parse(to));
        }
        if (sort != null) {
            if ("date".equals(sort)) {
                return requests.sortByDate();
            }
            if ("budget".equals(sort)) {
                return requests.sortByBudget();
            }
            throw new BusinessException("Параметр sort принимает значения date или budget.");
        }
        return requests.getAll();
    }

    private void statistics(HttpExchange exchange, String method) throws Exception {
        if (!"GET".equals(method)) {
            ApiResponses.methodNotAllowed(exchange);
            return;
        }
        Map<String, Object> stats = new LinkedHashMap<>();
        requests.getStatistics().forEach((key, value) -> stats.put(key.trim(), value));
        stats.put("Всего клиентов", clients.getAll().size());
        stats.put("Всего локаций", locations.getAll().size());
        ApiResponses.json(exchange, 200, stats);
    }

    private static void requireRequest(ShootingRequestBody body) throws BusinessException {
        if (body.clientId() == null || body.locationId() == null) {
            throw new BusinessException("Нужно указать clientId и locationId.");
        }
        if (body.shootingDate() == null || body.shootingDate().isBlank()) {
            throw new BusinessException("Дата съёмки не может быть пустой.");
        }
        if (body.durationHours() == null) {
            throw new BusinessException("Длительность съёмки должна быть больше нуля.");
        }
    }

    private static int capacity(LocationBody body) throws BusinessException {
        if (body.capacity() == null) {
            throw new BusinessException("Вместимость локации должна быть больше нуля.");
        }
        return body.capacity();
    }

    private static LocationType locationType(LocationBody body) {
        return LocationType.fromString(body.type());
    }
}
