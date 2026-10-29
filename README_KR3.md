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
- `GET|POST|PUT /api/locations` — справочник локаций
- `GET /api/analytics/summary`
- `GET /api/admin/users`, `GET /api/admin/audit`

## Тесты

```bash
cd backend && mvn test
```

Покрыты: smoke Spring Context, расчёт приоритета, умный фильтр, политика паролей.

## Структура `backend/`

Пакеты: `controller`, `service`, `repository`, `domain`, `dto`, `security`, `filter`, `error`, `config`.  
Миграции Flyway: `src/main/resources/db/migration`.
