package ru.mirea.kinostudio;

import ru.mirea.kinostudio.exception.BusinessException;
import ru.mirea.kinostudio.exception.EntityNotFoundException;
import ru.mirea.kinostudio.model.Client;
import ru.mirea.kinostudio.model.Location;
import ru.mirea.kinostudio.model.LocationType;
import ru.mirea.kinostudio.model.RequestStatus;
import ru.mirea.kinostudio.model.ShootingRequest;
import ru.mirea.kinostudio.repository.ClientRepository;
import ru.mirea.kinostudio.repository.LocationRepository;
import ru.mirea.kinostudio.repository.ShootingRequestRepository;
import ru.mirea.kinostudio.service.ClientService;
import ru.mirea.kinostudio.service.LocationService;
import ru.mirea.kinostudio.service.ShootingRequestService;
import ru.mirea.kinostudio.util.ConsoleUtils;
import ru.mirea.kinostudio.util.ExcelExporter;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class Main {

    private static final Scanner scanner = new Scanner(System.in);

    private static final ClientRepository clientRepository = new ClientRepository();
    private static final LocationRepository locationRepository = new LocationRepository();
    private static final ShootingRequestRepository requestRepository = new ShootingRequestRepository();

    private static final ClientService clientService = new ClientService(clientRepository);
    private static final LocationService locationService = new LocationService(locationRepository);
    private static final ShootingRequestService requestService =
            new ShootingRequestService(requestRepository, clientRepository, locationRepository);

    public static void main(String[] args) {
        boolean running = true;
        while (running) {
            printMainMenu();
            int choice = ConsoleUtils.readInt(scanner, "Выберите действие: ");
            try {
                switch (choice) {
                    case 1 -> clientsMenu();
                    case 2 -> locationsMenu();
                    case 3 -> requestsMenu();
                    case 4 -> searchMenu();
                    case 5 -> filterMenu();
                    case 6 -> statisticsMenu();
                    case 7 -> exportMenu();
                    case 8 -> printTables();
                    case 0 -> running = false;
                    default -> System.out.println("Неизвестный пункт меню.");
                }
            } catch (SQLException e) {
                System.out.println("Ошибка базы данных: " + e.getMessage());
            }
        }
        System.out.println("Работа программы завершена.");
    }

    private static void printMainMenu() {
        System.out.println("""

                ========================================
                СИСТЕМА КИНОСТУДИИ — ЗАЯВКИ НА СЪЁМКУ
                ========================================
                1. Клиенты
                2. Локации (павильоны и площадки)
                3. Заявки на съёмку
                4. Поиск
                5. Фильтрация
                6. Статистика
                7. Экспорт данных
                8. Вывести таблицы базы данных
                0. Выход
                """);
    }

    // ================= КЛИЕНТЫ =================

    private static void clientsMenu() throws SQLException {
        System.out.println("""
                --- Клиенты ---
                1. Добавить клиента
                2. Показать всех клиентов
                3. Найти клиента по ID
                4. Изменить клиента
                5. Удалить клиента
                0. Назад
                """);
        int choice = ConsoleUtils.readInt(scanner, "Выберите действие: ");
        try {
            switch (choice) {
                case 1 -> addClient();
                case 2 -> printClients(clientService.getAll());
                case 3 -> {
                    int id = ConsoleUtils.readInt(scanner, "Введите ID клиента: ");
                    System.out.println(clientService.getById(id));
                }
                case 4 -> updateClient();
                case 5 -> {
                    int id = ConsoleUtils.readInt(scanner, "Введите ID клиента: ");
                    clientService.delete(id);
                    System.out.println("Клиент удалён.");
                }
                case 0 -> { /* назад */ }
                default -> System.out.println("Неизвестный пункт меню.");
            }
        } catch (BusinessException e) {
            System.out.println("Нарушение бизнес-правила: " + e.getMessage());
        } catch (EntityNotFoundException e) {
            System.out.println("Запись не найдена: " + e.getMessage());
        }
    }

    private static void addClient() throws SQLException, BusinessException {
        String fullName = ConsoleUtils.readLine(scanner, "ФИО клиента: ");
        String email = ConsoleUtils.readLine(scanner, "Email: ");
        String phone = ConsoleUtils.readLine(scanner, "Телефон: ");
        Client c = clientService.create(fullName, email, phone);
        System.out.println("Клиент создан: " + c);
    }

    private static void updateClient() throws SQLException, BusinessException, EntityNotFoundException {
        int id = ConsoleUtils.readInt(scanner, "Введите ID клиента: ");
        String fullName = ConsoleUtils.readLine(scanner, "Новое ФИО: ");
        String email = ConsoleUtils.readLine(scanner, "Новый email: ");
        String phone = ConsoleUtils.readLine(scanner, "Новый телефон: ");
        clientService.update(id, fullName, email, phone);
        System.out.println("Клиент обновлён.");
    }

    private static void printClients(List<Client> clients) {
        if (clients.isEmpty()) {
            System.out.println("Список клиентов пуст.");
            return;
        }
        clients.forEach(System.out::println);
    }

    // ================= ЛОКАЦИИ =================

    private static void locationsMenu() throws SQLException {
        System.out.println("""
                --- Локации ---
                1. Добавить локацию
                2. Показать все локации
                3. Найти локацию по ID
                4. Изменить локацию
                5. Удалить локацию
                0. Назад
                """);
        int choice = ConsoleUtils.readInt(scanner, "Выберите действие: ");
        try {
            switch (choice) {
                case 1 -> addLocation();
                case 2 -> printLocations(locationService.getAll());
                case 3 -> {
                    int id = ConsoleUtils.readInt(scanner, "Введите ID локации: ");
                    System.out.println(locationService.getById(id));
                }
                case 4 -> updateLocation();
                case 5 -> {
                    int id = ConsoleUtils.readInt(scanner, "Введите ID локации: ");
                    locationService.delete(id);
                    System.out.println("Локация удалена.");
                }
                case 0 -> { /* назад */ }
                default -> System.out.println("Неизвестный пункт меню.");
            }
        } catch (BusinessException e) {
            System.out.println("Нарушение бизнес-правила: " + e.getMessage());
        } catch (EntityNotFoundException e) {
            System.out.println("Запись не найдена: " + e.getMessage());
        }
    }

    private static void addLocation() throws SQLException, BusinessException {
        String name = ConsoleUtils.readLine(scanner, "Название локации: ");
        String address = ConsoleUtils.readLine(scanner, "Адрес: ");
        int capacity = ConsoleUtils.readInt(scanner, "Вместимость: ");
        LocationType type = readLocationType();
        Location loc = locationService.create(name, address, capacity, type);
        System.out.println("Локация создана: " + loc);
    }

    private static void updateLocation() throws SQLException, BusinessException, EntityNotFoundException {
        int id = ConsoleUtils.readInt(scanner, "Введите ID локации: ");
        String name = ConsoleUtils.readLine(scanner, "Новое название: ");
        String address = ConsoleUtils.readLine(scanner, "Новый адрес: ");
        int capacity = ConsoleUtils.readInt(scanner, "Новая вместимость: ");
        LocationType type = readLocationType();
        locationService.update(id, name, address, capacity, type);
        System.out.println("Локация обновлена.");
    }

    private static LocationType readLocationType() {
        while (true) {
            String input = ConsoleUtils.readLine(scanner, "Тип (PAVILION/OUTDOOR/STUDIO/OFFICE): ");
            try {
                return LocationType.fromString(input);
            } catch (IllegalArgumentException e) {
                System.out.println("Ошибка: " + e.getMessage());
            }
        }
    }

    private static void printLocations(List<Location> locations) {
        if (locations.isEmpty()) {
            System.out.println("Список локаций пуст.");
            return;
        }
        locations.forEach(System.out::println);
    }

    // ================= ЗАЯВКИ =================

    private static void requestsMenu() throws SQLException {
        System.out.println("""
                --- Заявки на съёмку ---
                1. Создать заявку
                2. Показать все заявки
                3. Найти заявку по ID
                4. Изменить заявку
                5. Удалить заявку
                6. Изменить статус заявки
                0. Назад
                """);
        int choice = ConsoleUtils.readInt(scanner, "Выберите действие: ");
        try {
            switch (choice) {
                case 1 -> addRequest();
                case 2 -> printRequests(requestService.getAll());
                case 3 -> {
                    int id = ConsoleUtils.readInt(scanner, "Введите ID заявки: ");
                    System.out.println(requestService.getById(id));
                }
                case 4 -> updateRequest();
                case 5 -> {
                    int id = ConsoleUtils.readInt(scanner, "Введите ID заявки: ");
                    requestService.delete(id);
                    System.out.println("Заявка удалена.");
                }
                case 6 -> changeRequestStatus();
                case 0 -> { /* назад */ }
                default -> System.out.println("Неизвестный пункт меню.");
            }
        } catch (BusinessException e) {
            System.out.println("Нарушение бизнес-правила: " + e.getMessage());
        } catch (EntityNotFoundException e) {
            System.out.println("Запись не найдена: " + e.getMessage());
        }
    }

    private static void addRequest() throws SQLException, BusinessException {
        int clientId = ConsoleUtils.readInt(scanner, "ID клиента: ");
        int locationId = ConsoleUtils.readInt(scanner, "ID локации (см. пункт меню 2): ");
        String title = ConsoleUtils.readLine(scanner, "Название заявки: ");
        String description = ConsoleUtils.readLine(scanner, "Описание: ");
        LocalDate date = ConsoleUtils.readDate(scanner, "Дата съёмки");
        int duration = ConsoleUtils.readInt(scanner, "Длительность (часов): ");
        BigDecimal budget = ConsoleUtils.readBigDecimal(scanner, "Бюджет: ");

        ShootingRequest r = requestService.create(clientId, locationId, title, description, date, duration, budget);
        System.out.println("Заявка создана: " + r);
    }

    private static void updateRequest() throws SQLException, BusinessException, EntityNotFoundException {
        int id = ConsoleUtils.readInt(scanner, "Введите ID заявки: ");
        int clientId = ConsoleUtils.readInt(scanner, "Новый ID клиента: ");
        int locationId = ConsoleUtils.readInt(scanner, "Новый ID локации: ");
        String title = ConsoleUtils.readLine(scanner, "Новое название: ");
        String description = ConsoleUtils.readLine(scanner, "Новое описание: ");
        LocalDate date = ConsoleUtils.readDate(scanner, "Новая дата съёмки");
        int duration = ConsoleUtils.readInt(scanner, "Новая длительность (часов): ");
        BigDecimal budget = ConsoleUtils.readBigDecimal(scanner, "Новый бюджет: ");

        requestService.update(id, clientId, locationId, title, description, date, duration, budget);
        System.out.println("Заявка обновлена.");
    }

    private static void changeRequestStatus() throws SQLException, BusinessException, EntityNotFoundException {
        int id = ConsoleUtils.readInt(scanner, "Введите ID заявки: ");
        System.out.println("Доступные статусы: NEW, APPROVED, IN_PROGRESS, COMPLETED, REJECTED, CANCELLED");
        String statusInput = ConsoleUtils.readLine(scanner, "Новый статус: ");
        RequestStatus status;
        try {
            status = RequestStatus.fromString(statusInput);
        } catch (IllegalArgumentException e) {
            System.out.println("Ошибка: " + e.getMessage());
            return;
        }
        requestService.changeStatus(id, status);
        System.out.println("Статус заявки обновлён.");
    }

    private static void printRequests(List<ShootingRequest> requests) {
        if (requests.isEmpty()) {
            System.out.println("Заявки не найдены.");
            return;
        }
        requests.forEach(System.out::println);
    }

    // ================= ПОИСК =================

    private static void searchMenu() throws SQLException {
        System.out.println("""
                --- Поиск ---
                1. По названию заявки
                2. По локации
                0. Назад
                """);
        int choice = ConsoleUtils.readInt(scanner, "Выберите действие: ");
        switch (choice) {
            case 1 -> {
                String keyword = ConsoleUtils.readLine(scanner, "Ключевое слово в названии: ");
                printRequests(requestService.searchByTitle(keyword));
            }
            case 2 -> {
                String keyword = ConsoleUtils.readLine(scanner, "Ключевое слово в локации: ");
                printRequests(requestService.searchByLocation(keyword));
            }
            case 0 -> { /* назад */ }
            default -> System.out.println("Неизвестный пункт меню.");
        }
    }

    // ================= ФИЛЬТРАЦИЯ =================

    private static void filterMenu() throws SQLException {
        System.out.println("""
                --- Фильтрация ---
                1. По статусу
                2. По диапазону дат
                3. Сортировка по дате съёмки
                4. Сортировка по бюджету
                0. Назад
                """);
        int choice = ConsoleUtils.readInt(scanner, "Выберите действие: ");
        try {
            switch (choice) {
                case 1 -> {
                    String statusInput = ConsoleUtils.readLine(scanner, "Статус (NEW/APPROVED/IN_PROGRESS/COMPLETED/REJECTED/CANCELLED): ");
                    RequestStatus status = RequestStatus.fromString(statusInput);
                    printRequests(requestService.filterByStatus(status));
                }
                case 2 -> {
                    LocalDate from = ConsoleUtils.readDate(scanner, "Дата начала");
                    LocalDate to = ConsoleUtils.readDate(scanner, "Дата окончания");
                    printRequests(requestService.filterByDateRange(from, to));
                }
                case 3 -> printRequests(requestService.sortByDate());
                case 4 -> printRequests(requestService.sortByBudget());
                case 0 -> { /* назад */ }
                default -> System.out.println("Неизвестный пункт меню.");
            }
        } catch (IllegalArgumentException e) {
            System.out.println("Ошибка: " + e.getMessage());
        } catch (BusinessException e) {
            System.out.println("Нарушение бизнес-правила: " + e.getMessage());
        }
    }

    // ================= СТАТИСТИКА =================

    private static void statisticsMenu() throws SQLException {
        System.out.println("--- Статистика системы ---");
        Map<String, Object> stats = requestService.getStatistics();
        stats.forEach((key, value) -> System.out.printf("%-40s: %s%n", key, value));
        System.out.println("Всего клиентов                        : " + clientService.getAll().size() + "\n"
                + "Всего локаций                          : " + locationService.getAll().size());
    }

    // ================= ЭКСПОРТ =================

    private static void exportMenu() throws SQLException {
        String filePath = "kinostudio_export.xlsx";
        try {
            List<ShootingRequest> requests = requestService.getAll();
            List<Client> clients = clientService.getAll();
            List<Location> locations = locationService.getAll();
            ExcelExporter.exportRequests(requests, clients, locations, filePath);
            System.out.println("Данные экспортированы в файл: " + filePath);
        } catch (Exception e) {
            System.out.println("Ошибка экспорта: " + e.getMessage());
        }
    }

    // ================= ТАБЛИЦЫ БД =================

    private static void printTables() {
        System.out.println("Таблицы базы данных:\n"
                + " - clients (клиенты)\n"
                + " - locations (локации: павильоны и площадки)\n"
                + " - shooting_requests (заявки на съёмку)");
    }
}
