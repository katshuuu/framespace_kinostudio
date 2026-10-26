package ru.mirea.kinostudio.api;

import ru.mirea.kinostudio.api.auth.AuthResponse;
import ru.mirea.kinostudio.api.auth.AuthService;
import ru.mirea.kinostudio.api.auth.JwtService;
import ru.mirea.kinostudio.api.auth.PasswordHasher;
import ru.mirea.kinostudio.repository.ClientRepository;
import ru.mirea.kinostudio.repository.LocationRepository;
import ru.mirea.kinostudio.repository.ShootingRequestRepository;
import ru.mirea.kinostudio.repository.UserRepository;
import ru.mirea.kinostudio.service.ClientService;
import ru.mirea.kinostudio.service.LocationService;
import ru.mirea.kinostudio.service.ShootingRequestService;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * Отдельная точка входа REST API. Консольный класс Main не заменяется.
 */
public final class ApiServer {

    static final int PORT = 7070;
    private static final long TOKEN_TTL_SECONDS = 3600;

    private final AuthService auth;
    private final JwtService jwt;
    private final StudioResources resources;

    public ApiServer(AuthService auth, JwtService jwt, StudioResources resources) {
        this.auth = auth;
        this.jwt = jwt;
        this.resources = resources;
    }

    public static void main(String[] args) {
        String secret = System.getenv("JWT_SECRET");
        if (secret == null || secret.length() < 32) {
            System.err.println("Задайте переменную окружения JWT_SECRET длиной не меньше 32 символов.");
            System.exit(1);
            return;
        }
        try {
            UserRepository users = new UserRepository();
            users.ensureSchema();
            JwtService jwt = new JwtService(secret, TOKEN_TTL_SECONDS);
            AuthService auth = new AuthService(users, jwt, new PasswordHasher());
            if (users.findByUsername("admin").isEmpty()) {
                auth.register("admin", "admin123", "Администратор");
            }
            ClientRepository clientRepository = new ClientRepository();
            LocationRepository locationRepository = new LocationRepository();
            ShootingRequestRepository requestRepository = new ShootingRequestRepository();
            StudioResources resources = new StudioResources(
                    new ClientService(clientRepository),
                    new LocationService(locationRepository),
                    new ShootingRequestService(requestRepository, clientRepository, locationRepository));
            HttpServer server = new ApiServer(auth, jwt, resources).start();
            System.out.println("FrameSpace API: http://localhost:" + PORT);
            System.out.println("Демонстрационный вход, если логин ещё не был создан: admin / admin123");
            Runtime.getRuntime().addShutdownHook(new Thread(() -> server.stop(0)));
        } catch (SQLException e) {
            System.err.println("Ошибка базы данных: " + e.getMessage());
            System.err.println("Проверьте, что PostgreSQL запущен, база kinostudio_db создана, а константы");
            System.err.println("в DatabaseManager совпадают с локальной установкой.");
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Не удалось запустить API: " + e.getMessage());
            System.exit(1);
        }
    }

    HttpServer start() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", PORT), 0);
        server.createContext("/api", this::handle);
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();
        return server;
    }

    private void handle(HttpExchange exchange) {
        try {
            dispatch(exchange);
        } catch (Exception e) {
            try {
                if (exchange.getResponseCode() == -1) {
                    ApiResponses.sendError(exchange, e);
                }
            } catch (Exception nested) {
                nested.printStackTrace(System.err);
            }
        } finally {
            exchange.close();
        }
    }

    private void dispatch(HttpExchange exchange) throws Exception {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        if ("/api/health".equals(path)) {
            if ("GET".equals(method)) {
                ApiResponses.json(exchange, 200, Map.of("status", "ok"));
            } else {
                ApiResponses.methodNotAllowed(exchange);
            }
            return;
        }
        if ("/api/auth/register".equals(path)) {
            if ("POST".equals(method)) {
                RegisterBody body = ApiJson.read(exchange, RegisterBody.class);
                AuthResponse response = auth.register(body.username(), body.password(), body.fullName());
                ApiResponses.json(exchange, 201, response);
            } else {
                ApiResponses.methodNotAllowed(exchange);
            }
            return;
        }
        if ("/api/auth/login".equals(path)) {
            if ("POST".equals(method)) {
                LoginBody body = ApiJson.read(exchange, LoginBody.class);
                ApiResponses.json(exchange, 200, auth.login(body.username(), body.password()));
            } else {
                ApiResponses.methodNotAllowed(exchange);
            }
            return;
        }
        if (!authorized(exchange)) {
            return;
        }
        resources.handle(exchange, method, path);
    }

    private boolean authorized(HttpExchange exchange) throws IOException {
        String header = exchange.getRequestHeaders().getFirst("Authorization");
        if (header == null || !header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            ApiResponses.json(exchange, 401, Map.of(
                    "error", "Требуется заголовок Authorization: Bearer <token>."));
            return false;
        }
        String token = header.substring(7).trim();
        if (token.isEmpty()) {
            ApiResponses.json(exchange, 401, Map.of(
                    "error", "Требуется заголовок Authorization: Bearer <token>."));
            return false;
        }
        try {
            jwt.verify(token);
            return true;
        } catch (JWTVerificationException e) {
            ApiResponses.json(exchange, 401, Map.of("error", "Токен недействителен или истёк."));
            return false;
        }
    }
}
