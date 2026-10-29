# КР №3 — FrameSpace backend (Spring Boot)

Spring Boot REST API для информационной системы киностудии: заявки на съёмку, локации, JWT, PostgreSQL, Flyway, Docker Compose и web-интерфейс для проверки.

Предметная область продолжается с КР №1/КР №2. JavaFX-клиент остаётся отдельным; этот backend даёт API для Android (КР №4).

## Соответствие Student Hub → FrameSpace

| Student Hub | FrameSpace |
|---|---|
| Student | ShootingRequest (заявка) |
| StudyGroup | Location (локация) |
| StudentStatus | RequestStatus |
| RiskLevel | PriorityLevel |
| StudentNote | RequestNote |
| student_tag | request_tag |
| TEACHER | OPERATOR |
| — | Client (клиент — заказчик съёмки, таблица `clients` из КР №1) |

## Связь с КР №1 и КР №2

- Схема повторяет модель КР №1: клиенты (`client`), локации с типом (`PAVILION`, `STUDIO`, `OUTDOOR`, `OFFICE`), заявки со ссылками на клиента и локацию.
- Тестовые данные КР №1 (5 клиентов, 8 локаций, 10 заявок) загружает миграция `V3__kr1_seed_data.sql`; у заявок `externalId` `KR1-001` … `KR1-010`.
- Бизнес-правила КР №2 перенесены на сервер:
  - новая заявка создаётся только в статусе `NEW`;
  - дата съёмки не может быть в прошлом (проверяется при создании и при изменении даты);
  - клиент и локация должны существовать;
  - статус меняется только по графу:

```
NEW → APPROVED | REJECTED
APPROVED → IN_PROGRESS | CANCELLED
IN_PROGRESS → COMPLETED | CANCELLED
COMPLETED, REJECTED, CANCELLED — конечные
```

Недопустимый переход возвращает 409. В карточке заявки поле `nextStatuses` перечисляет допустимые следующие статусы.

## Миграции Flyway

| Версия | Что делает |
|---|---|
| `V1__initial_schema.sql` | Пользователи, refresh-сессии, локации, заявки, теги, заметки, аудит |
| `V2__clients_and_location_type.sql` | Таблица `client`, перенос клиентов из уже существующих заявок, `client_id` в заявке, тип локации, `CHECK` на статус и вместимость |
| `V3__kr1_seed_data.sql` | Данные КР №1; повторный запуск не затирает записи, созданные вручную |

## Требования

- Docker Desktop
- JDK 21
- Maven 3.9+

## Запуск

1. Скопируйте переменные окружения:

```bash
cp .env.example .env
```

Заполните пароли и `JWT_SECRET_BASE64` своими значениями.

2. Запустите Docker Desktop.

3. Из корня репозитория:

```bash
./backend/start-compose.sh
```

Windows (PowerShell):

```powershell
powershell -ExecutionPolicy Bypass -File .\backend\start-compose.ps1
```

Или напрямую:

```bash
docker compose --env-file .env -f backend/docker-compose.yml up -d --wait
```

## Адреса

| Назначение | Адрес |
|---|---|
| Web UI | http://localhost:8080/ |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| REST API | http://localhost:8080/api |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |
| Health | http://localhost:8080/actuator/health |
| PostgreSQL | jdbc:postgresql://localhost:55432/framespace |

Администратор задаётся в `.env` (`BOOTSTRAP_ADMIN_EMAIL` / `BOOTSTRAP_ADMIN_PASSWORD`). После первого входа система потребует сменить временный пароль.

## Основные API

- `POST /api/auth/login`, `/refresh`, `/logout`, `GET /api/auth/me`
- `GET|POST|PUT|DELETE /api/requests` — заявки, архив, restore, permanent, bulk, CSV
- `GET|POST|PUT|DELETE /api/clients` — клиенты; удалить можно только клиента без заявок (ADMIN)
- `GET|POST|PUT /api/locations` — справочник локаций
- `GET /api/analytics/summary`
- `GET /api/admin/users`, `GET /api/admin/audit`

## Тесты

```bash
cd backend && mvn test
```

Покрыты: smoke Spring Context, расчёт приоритета, граф статусов, умный фильтр, политика паролей, совпадение версии в ответе и в базе.

## Структура `backend/`

Пакеты: `controller`, `service`, `repository`, `domain`, `dto`, `security`, `filter`, `error`, `config`.  
Миграции Flyway: `src/main/resources/db/migration`.
