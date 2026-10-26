# ER-диаграмма БД «Киностудия — Заявки на съёмку»

```mermaid
erDiagram
    КЛИЕНТЫ ||--o{ ЗАЯВКИ_НА_СЪЕМКУ : "оформляет"
    ЛОКАЦИИ ||--o{ ЗАЯВКИ_НА_СЪЕМКУ : "принимает"

    КЛИЕНТЫ {
        int id PK
        varchar фио
        varchar email UK
        varchar телефон
        timestamp дата_создания
    }

    ЛОКАЦИИ {
        int id PK
        varchar название UK
        varchar адрес
        int вместимость
        varchar тип
        timestamp дата_создания
    }

    ЗАЯВКИ_НА_СЪЕМКУ {
        int id PK
        int id_клиента FK
        int id_локации FK
        varchar название
        text описание
        date дата_съемки
        int длительность_часов
        numeric бюджет
        varchar статус
        timestamp дата_создания
    }

    ПОЛЬЗОВАТЕЛИ {
        int id PK
        varchar логин UK
        varchar хеш_пароля
        varchar роль
        timestamp дата_создания
    }
```

> Названия сущностей и полей на диаграмме даны на русском для читаемости.
> Реальные имена таблиц и колонок в БД остаются латиницей (см. `sql/schema.sql` и `sql/users.sql`):
> `КЛИЕНТЫ` → `clients`, `ЛОКАЦИИ` → `locations`, `ЗАЯВКИ_НА_СЪЕМКУ` → `shooting_requests`, `ПОЛЬЗОВАТЕЛИ` → `users`.

## Связи
- `shooting_requests.client_id` → `clients.id` (внешний ключ, `ON DELETE CASCADE`). Один клиент — много заявок (1:N).
- `shooting_requests.location_id` → `locations.id` (внешний ключ, `ON DELETE RESTRICT`, чтобы нельзя было удалить локацию, на которую есть активные заявки). Одна локация — много заявок (1:N).
- Таблица `users` ни с одной таблицей не связана: она относится только к входу в систему и не участвует в учёте заявок.

## Сущность «Локации»
Хранит павильоны и натурные площадки студии:
- `название` (`name`) — название площадки (например, «Павильон №1»), уникально.
- `адрес` (`address`) — физический адрес.
- `вместимость` (`capacity`) — вместимость (кв. м или человек).
- `тип` (`type`) — тип локации (`PAVILION` / `OUTDOOR` / `STUDIO` / `OFFICE`, хранится как varchar).

## Сущность «Пользователи» (КР №2)
Хранит учётные записи обычных пользователей, которые регистрируются в приложении:
- `логин` (`login`) — от 3 до 30 символов, уникален без учёта регистра (уникальный индекс по `LOWER(login)`).
- `хеш_пароля` (`password_hash`) — хеш PBKDF2WithHmacSHA256 в формате `итерации:соль:хеш`, сам пароль не хранится.
- `роль` (`role`) — `USER` или `ADMIN` (ограничение `CHECK`).
- Администратор в таблице не хранится: его логин и пароль задаются в файле `.env` (`ADMIN_LOGIN`, `ADMIN_PASSWORD`).

## Ограничения
- `clients.email` — уникален (`UNIQUE`).
- `shooting_requests.duration_hours > 0` (`CHECK`).
- `shooting_requests.budget >= 0` (`CHECK`).
- `shooting_requests.status` — ограничен допустимыми значениями enum (`CHECK ... IN (...)`).
- `users.role IN ('USER', 'ADMIN')` (`CHECK`).

## Граф допустимых переходов статуса заявки

```mermaid
stateDiagram-v2
    [*] --> NEW
    NEW --> APPROVED
    NEW --> REJECTED
    APPROVED --> IN_PROGRESS
    APPROVED --> CANCELLED
    IN_PROGRESS --> COMPLETED
    IN_PROGRESS --> CANCELLED
    COMPLETED --> [*]
    REJECTED --> [*]
    CANCELLED --> [*]
```

| Из статуса | Можно перейти в |
|---|---|
| `NEW` | `APPROVED`, `REJECTED` |
| `APPROVED` | `IN_PROGRESS`, `CANCELLED` |
| `IN_PROGRESS` | `COMPLETED`, `CANCELLED` |
| `COMPLETED`, `REJECTED`, `CANCELLED` | никуда (терминальные) |

Правило реализовано в `ShootingRequestService` (`ALLOWED_TRANSITIONS`, метод `changeStatus`).