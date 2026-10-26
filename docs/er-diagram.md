# ER-диаграмма БД «Киностудия — Заявки на съёмку»

```mermaid
erDiagram
    КЛИЕНТЫ ||--o{ ЗАЯВКИ_НА_СЪЕМКУ : "оформляет"
    ЛОКАЦИИ ||--o{ ЗАЯВКИ_НА_СЪЕМКУ : "принимает"

    СОТРУДНИКИ {
        int id PK
        varchar логин UK
        varchar хеш_пароля
        varchar фио
        timestamp дата_создания
    }

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
```

> Названия сущностей и полей на диаграмме даны на русском для читаемости.
> Реальные имена таблиц/колонок в БД остаются латиницей (см. `sql/schema.sql`):
> `КЛИЕНТЫ` → `clients`, `ЛОКАЦИИ` → `locations`, `ЗАЯВКИ_НА_СЪЕМКУ` → `shooting_requests`, `СОТРУДНИКИ` → `users`.

## Связи
- `shooting_requests.client_id` → `clients.id` (внешний ключ, `ON DELETE CASCADE`). Один клиент — много заявок (1:N).
- `shooting_requests.location_id` → `locations.id` (внешний ключ, `ON DELETE RESTRICT`, чтобы нельзя было удалить локацию, на которую есть активные заявки). Одна локация — много заявок (1:N).
- `users` не ссылается на `clients`. Сотрудник API и заказчик съёмки — разные сущности. Пароль хранится только как bcrypt-хеш в `password_hash`. Определение таблицы — `sql/users.sql`, в `sql/schema.sql` его нет.

## Сущность «Локации»
Хранит павильоны и натурные площадки студии:
- `название` (`name`) — название площадки (например, «Павильон №1»), уникально.
- `адрес` (`address`) — физический адрес.
- `вместимость` (`capacity`) — вместимость (кв. м или человек).
- `тип` (`type`) — тип локации (`PAVILION` / `OUTDOOR` / `STUDIO` / `OFFICE`, хранится как varchar).

## Ограничения
- `clients.email` — уникален (`UNIQUE`).
- `shooting_requests.duration_hours > 0` (`CHECK`).
- `shooting_requests.budget >= 0` (`CHECK`).
- `shooting_requests.status` — ограничен допустимыми значениями enum (`CHECK ... IN (...)`).

## Граф допустимых переходов статуса заявки
```
NEW ──▶ APPROVED ──▶ IN_PROGRESS ──▶ COMPLETED
 │            │
 └──▶ REJECTED └──▶ CANCELLED
```
Из терминальных статусов (`COMPLETED`, `REJECTED`, `CANCELLED`) переходы запрещены.
