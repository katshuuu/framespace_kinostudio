package ru.mirea.framespace.service;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.mirea.framespace.config.AppProperties;
import ru.mirea.framespace.domain.Client;
import ru.mirea.framespace.domain.ShootingRequest;
import ru.mirea.framespace.domain.RequestStatus;
import ru.mirea.framespace.domain.Location;
import ru.mirea.framespace.domain.UserAccount;
import ru.mirea.framespace.dto.ClientDtos.ClientUpsert;
import ru.mirea.framespace.dto.ImportDtos.ImportMode;
import ru.mirea.framespace.dto.ImportDtos.ImportPreview;
import ru.mirea.framespace.dto.ImportDtos.ImportResult;
import ru.mirea.framespace.dto.ImportDtos.RowIssue;
import ru.mirea.framespace.dto.RequestDtos.RequestUpsert;
import ru.mirea.framespace.dto.RequestDtos.RequestResponse;
import ru.mirea.framespace.error.BadRequestException;
import ru.mirea.framespace.filter.SmartFilterParser;
import ru.mirea.framespace.filter.RequestSpecifications;
import ru.mirea.framespace.repository.ClientRepository;
import ru.mirea.framespace.repository.ShootingRequestRepository;
import ru.mirea.framespace.repository.LocationRepository;
import ru.mirea.framespace.security.CurrentUserService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Защищённый CSV import/export заявок.
 *
 * <p>Предварительная проверка отделена от импорта: ошибки видны до записи в базу.
 * При экспорте нейтрализуются значения, которые Excel может воспринять как формулы.</p>
 */
@Service
public class CsvRequestService {
    private static final String[] HEADERS = {
            "externalId", "title", "description", "clientName", "clientEmail", "clientPhone",
            "shootingDate", "location", "status", "budget", "durationHours", "urgent", "tags"
    };

    private final ShootingRequestRepository requests;
    private final LocationRepository locations;
    private final ClientRepository clients;
    private final ClientService clientService;
    private final RequestService requestService;
    private final CurrentUserService currentUser;
    private final SmartFilterParser parser;
    private final RequestMapper mapper;
    private final Validator validator;
    private final AppProperties properties;
    private final AuditService audit;
    private final Clock clock;

    public CsvRequestService(ShootingRequestRepository requests, LocationRepository locations,
                             ClientRepository clients, ClientService clientService,
                             RequestService requestService, CurrentUserService currentUser,
                             SmartFilterParser parser, RequestMapper mapper, Validator validator,
                             AppProperties properties, AuditService audit, Clock clock) {
        this.requests = requests;
        this.locations = locations;
        this.clients = clients;
        this.clientService = clientService;
        this.clock = clock;
        this.requestService = requestService;
        this.currentUser = currentUser;
        this.parser = parser;
        this.mapper = mapper;
        this.validator = validator;
        this.properties = properties;
        this.audit = audit;
    }

    /** Проверяет CSV без записи в базу. */
    @Transactional(readOnly = true)
    public ImportPreview preview(MultipartFile file, ImportMode mode) {
        ParsedFile parsed = parse(file, mode == null ? ImportMode.CREATE_ONLY : mode);
        return new ImportPreview(parsed.totalRows(), parsed.rows().size(), parsed.issues().size(), parsed.issues());
    }

    /** Применяет только валидные строки и возвращает детальный итог. */
    @Transactional
    public ImportResult apply(MultipartFile file, ImportMode mode) {
        ImportMode effectiveMode = mode == null ? ImportMode.CREATE_ONLY : mode;
        ParsedFile parsed = parse(file, effectiveMode);
        UserAccount actor = currentUser.requireEntity();
        int created = 0;
        int updated = 0;
        int skipped = parsed.issues().size();
        List<RowIssue> runtimeIssues = new ArrayList<>(parsed.issues());

        for (ParsedRow row : parsed.rows()) {
            try {
                ShootingRequest existing = requests.findByExternalIdIgnoreCase(row.request().externalId()).orElse(null);
                if (existing != null && effectiveMode == ImportMode.CREATE_ONLY) {
                    runtimeIssues.add(new RowIssue(row.rowNumber(), "externalId", "Запись уже существует"));
                    skipped++;
                    continue;
                }
                RequestUpsert request = row.newClient() == null ? row.request()
                        : row.request().withClientId(clientService.findOrCreateForImport(row.newClient()).getId());
                requestService.upsertFromImport(existing, request, actor);
                if (existing == null) created++; else updated++;
            } catch (RuntimeException ex) {
                runtimeIssues.add(new RowIssue(row.rowNumber(), "row", safeMessage(ex)));
                skipped++;
            }
        }
        audit.record("REQUEST_CSV_IMPORTED", "REQUEST", null, "Импортирован CSV с заявками",
                Map.of("created", created, "updated", updated, "skipped", skipped, "mode", effectiveMode.name()));
        return new ImportResult(parsed.totalRows(), created, updated, skipped, List.copyOf(runtimeIssues));
    }

    /** Потоково экспортирует текущую выборку без загрузки всего набора в память. */
    @Transactional(readOnly = true)
    public void export(String query, OutputStream output) throws IOException {
        output.write("\ufeff".getBytes(StandardCharsets.UTF_8));
        try (Writer writer = new OutputStreamWriter(output, StandardCharsets.UTF_8);
             CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder().setHeader(HEADERS).get())) {
            int page = 0;
            while (true) {
                var result = requests.findAll(RequestSpecifications.from(parser.parse(query)),
                        PageRequest.of(page, 500, Sort.by("title").ascending()));
                for (ShootingRequest entity : result.getContent()) {
                    RequestResponse dto = mapper.toResponse(entity);
                    printer.printRecord(
                            safeCell(dto.externalId()), safeCell(dto.title()), safeCell(dto.description()),
                            safeCell(dto.clientName()), safeCell(dto.clientEmail()), safeCell(dto.clientPhone()),
                            dto.shootingDate(), safeCell(dto.locationCode()), dto.status(), dto.budget(),
                            dto.durationHours(), dto.urgent(), safeCell(String.join("|", dto.tags())));
                }
                if (result.isLast()) break;
                page++;
            }
            printer.flush();
        }
        audit.record("REQUEST_CSV_EXPORTED", "REQUEST", null, "Экспортирована выборка заявок",
                Map.of("query", query == null ? "" : query));
    }

    private ParsedFile parse(MultipartFile file, ImportMode mode) {
        if (file == null || file.isEmpty()) throw new BadRequestException("Выберите непустой CSV-файл");
        List<ParsedRow> validRows = new ArrayList<>();
        List<RowIssue> issues = new ArrayList<>();
        Set<String> externalIdsInFile = new LinkedHashSet<>();
        int total = 0;

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .setTrim(true)
                .get();
        try (Reader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser csv = CSVParser.parse(skipBom(reader), format)) {
            validateHeaders(csv.getHeaderMap());
            for (CSVRecord record : csv) {
                total++;
                long rowNumber = record.getRecordNumber() + 1;
                if (total > properties.importLimits().maxRows()) {
                    throw new BadRequestException("CSV содержит больше " + properties.importLimits().maxRows() + " строк");
                }
                try {
                    ParsedRow row = toRow(record, rowNumber);
                    List<RowIssue> rowIssues = new ArrayList<>();
                    for (ConstraintViolation<RequestUpsert> violation : validator.validate(row.request())) {
                        String field = violation.getPropertyPath().toString();
                        if (row.newClient() != null && field.equals("clientId")) continue;
                        rowIssues.add(new RowIssue(rowNumber, field, violation.getMessage()));
                    }
                    if (row.newClient() != null) {
                        for (ConstraintViolation<ClientUpsert> violation : validator.validate(row.newClient())) {
                            rowIssues.add(new RowIssue(rowNumber, clientColumn(violation.getPropertyPath().toString()),
                                    violation.getMessage()));
                        }
                    }
                    if (rowIssues.isEmpty()) {
                        String businessIssue = businessIssue(row.request());
                        if (businessIssue != null) rowIssues.add(new RowIssue(rowNumber, "status", businessIssue));
                    }
                    if (!rowIssues.isEmpty()) {
                        issues.addAll(rowIssues);
                        continue;
                    }
                    String key = row.request().externalId().toLowerCase(Locale.ROOT);
                    if (!externalIdsInFile.add(key)) {
                        issues.add(new RowIssue(rowNumber, "externalId", "Дубликат externalId внутри файла"));
                        continue;
                    }
                    if (mode == ImportMode.CREATE_ONLY && requests.existsByExternalIdIgnoreCase(row.request().externalId())) {
                        issues.add(new RowIssue(rowNumber, "externalId", "Запись уже существует"));
                        continue;
                    }
                    validRows.add(row);
                } catch (RuntimeException ex) {
                    issues.add(new RowIssue(rowNumber, "row", safeMessage(ex)));
                }
            }
        } catch (IOException ex) {
            throw new BadRequestException("Не удалось прочитать CSV в UTF-8");
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Некорректная структура CSV: " + ex.getMessage());
        }
        return new ParsedFile(total, List.copyOf(validRows), List.copyOf(issues));
    }

    private ParsedRow toRow(CSVRecord record, long rowNumber) {
        String externalId = required(record, "externalId");
        String locationCode = nullable(record, "location");
        Location location = locationCode == null ? null : locations.findByCodeIgnoreCase(locationCode)
                .orElseThrow(() -> new BadRequestException("Локация не найдена: " + locationCode));
        if (location != null && !location.isActive()) throw new BadRequestException("Локация неактивна: " + locationCode);

        String clientEmail = required(record, "clientEmail");
        Client existingClient = clients.findByEmailIgnoreCase(clientEmail.trim()).orElse(null);
        ClientUpsert newClient = existingClient != null ? null : new ClientUpsert(
                required(record, "clientName"), clientEmail, required(record, "clientPhone"), null);

        RequestUpsert request = new RequestUpsert(
                externalId,
                required(record, "title"),
                required(record, "description"),
                existingClient == null ? null : existingClient.getId(),
                parseDate(required(record, "shootingDate")),
                location == null ? null : location.getId(),
                parseStatus(required(record, "status")),
                parseBudget(nullable(record, "budget")),
                parseDuration(nullable(record, "durationHours")),
                parseBoolean(nullable(record, "urgent")),
                parseTags(nullable(record, "tags")),
                null);
        return new ParsedRow(rowNumber, request, newClient);
    }

    /** Те же правила статуса и даты, что проверит RequestService, но без записи в базу. */
    private String businessIssue(RequestUpsert request) {
        ShootingRequest existing = requests.findByExternalIdIgnoreCase(request.externalId()).orElse(null);
        boolean pastDate = request.shootingDate().isBefore(LocalDate.now(clock));
        if (existing == null) {
            if (request.status() != RequestStatus.NEW) return "Новая заявка создаётся только в статусе «Новая»";
            if (pastDate) return "Дата съёмки не может быть в прошлом";
            return null;
        }
        if (!existing.getStatus().canMoveTo(request.status())) {
            return "Недопустимый переход статуса: " + existing.getStatus() + " → " + request.status();
        }
        if (pastDate && !request.shootingDate().equals(existing.getShootingDate())) {
            return "Дата съёмки не может быть в прошлом";
        }
        return null;
    }

    private static String clientColumn(String field) {
        return switch (field) {
            case "fullName" -> "clientName";
            case "email" -> "clientEmail";
            case "phone" -> "clientPhone";
            default -> field;
        };
    }

    /** Экспорт и Excel пишут BOM; без пропуска первая колонка читается как "\ufeffexternalId". */
    private static Reader skipBom(Reader reader) throws IOException {
        reader.mark(1);
        if (reader.read() != '\ufeff') reader.reset();
        return reader;
    }

    private static void validateHeaders(Map<String, Integer> actual) {
        for (String header : HEADERS) {
            if (!actual.containsKey(header)) throw new BadRequestException("Отсутствует колонка: " + header);
        }
    }

    private static String required(CSVRecord record, String field) {
        String value = nullable(record, field);
        if (value == null) throw new BadRequestException("Не заполнено поле " + field);
        return value;
    }

    private static String nullable(CSVRecord record, String field) {
        String value = record.get(field);
        if (value == null) return null;
        value = value.replace("\ufeff", "").trim();
        if (value.length() > 1 && value.charAt(0) == '\'' && "=+-@".indexOf(value.charAt(1)) >= 0) {
            value = value.substring(1);
        }
        return value.isEmpty() ? null : value;
    }

    private static LocalDate parseDate(String value) {
        if (value == null) return null;
        try { return LocalDate.parse(value); }
        catch (DateTimeParseException ex) { throw new BadRequestException("Дата должна иметь формат YYYY-MM-DD"); }
    }

    private static BigDecimal parseBudget(String value) {
        if (value == null) return null;
        try { return new BigDecimal(value.replace(',', '.')); }
        catch (NumberFormatException ex) { throw new BadRequestException("Некорректный budget"); }
    }

    private static Integer parseDuration(String value) {
        if (value == null) return null;
        try {
            int hours = Integer.parseInt(value);
            if (hours < 1) throw new BadRequestException("durationHours должен быть больше 0");
            return hours;
        } catch (NumberFormatException ex) {
            throw new BadRequestException("Некорректный durationHours");
        }
    }

    private static boolean parseBoolean(String value) {
        if (value == null) return false;
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "true", "1", "yes", "да" -> true;
            case "false", "0", "no", "нет" -> false;
            default -> throw new BadRequestException("urgent принимает true/false или да/нет");
        };
    }

    private static RequestStatus parseStatus(String value) {
        String normalized = value.toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        return switch (normalized) {
            case "new", "новая" -> RequestStatus.NEW;
            case "approved", "утверждена" -> RequestStatus.APPROVED;
            case "in_progress", "в_работе" -> RequestStatus.IN_PROGRESS;
            case "completed", "завершена" -> RequestStatus.COMPLETED;
            case "rejected", "отклонена" -> RequestStatus.REJECTED;
            case "cancelled", "canceled", "отменена" -> RequestStatus.CANCELLED;
            default -> throw new BadRequestException("Неизвестный status: " + value);
        };
    }

    private static Set<String> parseTags(String value) {
        if (value == null) return Set.of();
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        Arrays.stream(value.split("\\|")).map(String::trim).filter(v -> !v.isEmpty())
                .map(v -> v.toLowerCase(Locale.ROOT)).forEach(tags::add);
        return tags;
    }

    private static String safeCell(Object value) {
        if (value == null) return "";
        String cell = String.valueOf(value);
        if (!cell.isEmpty() && "=+-@\t\r".indexOf(cell.charAt(0)) >= 0) return "'" + cell;
        return cell;
    }

    private static String safeMessage(RuntimeException ex) {
        String message = ex.getMessage();
        return message == null || message.isBlank() ? "Некорректная строка" : message;
    }

    private record ParsedRow(long rowNumber, RequestUpsert request, ClientUpsert newClient) { }
    private record ParsedFile(int totalRows, List<ParsedRow> rows, List<RowIssue> issues) { }
}
