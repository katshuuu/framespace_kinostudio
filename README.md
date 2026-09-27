# Киностудия — заявки на съёмку

Консольная информационная система на Java для учёта клиентов киностудии, съёмочных локаций и заявок на съёмку. Данные хранятся в PostgreSQL. Доступ к ним выполняется через JDBC, бизнес-правила сосредоточены в сервисном слое, пользователь работает через интерактивное меню.

Контрольная работа 1. Предметная область сохраняется для последующих работ: консольное приложение, затем JavaFX, Spring REST и Android.

## Технологии

- Java 17
- Maven
- JDBC и драйвер PostgreSQL 42.7.3
- PostgreSQL
- Apache POI 5.2.5 — экспорт в Excel `.xlsx`

## Архитектура

```
Консольное меню → Сервис → Репозиторий (JDBC) → PostgreSQL
```

- Меню принимает ввод и показывает результат. SQL в меню отсутствует.
- Сервис проверяет бизнес-правила и решает, можно ли сохранить изменение.
- Репозиторий выполняет параметризованные запросы через `PreparedStatement`.
- База хранит данные и поддерживает ключи, `NOT NULL`, `UNIQUE` и `CHECK`.

## Структура репозитория

```
src/main/java/ru/mirea/kinostudio/
├── Main.java                          точка входа и консольное меню
├── model/
│   ├── Client.java                    клиент студии
│   ├── Location.java                  локация
│   ├── LocationType.java              тип локации
│   ├── ShootingRequest.java           заявка на съёмку
│   └── RequestStatus.java             статус заявки
├── repository/
│   ├── Repository.java                обобщённый интерфейс CRUD
│   ├── ClientRepository.java
│   ├── LocationRepository.java
│   └── ShootingRequestRepository.java
├── service/
│   ├── ClientService.java
│   ├── LocationService.java
│   └── ShootingRequestService.java
├── exception/
│   ├── BusinessException.java
│   └── EntityNotFoundException.java
└── util/
    ├── DatabaseManager.java           подключение к PostgreSQL
    ├── ConsoleUtils.java              безопасный ввод
    └── ExcelExporter.java             экспорт в .xlsx

sql/schema.sql                         схема и тестовые данные
docs/er-diagram.md                     ER-диаграмма
pom.xml
```

## Предметная модель

Три связанные сущности:

| Таблица | Назначение | Связь |
|---|---|---|
| `clients` | заказчик съёмки | один клиент — много заявок |
| `locations` | павильон, студия, натура или офис | одна локация — много заявок |
| `shooting_requests` | заявка на съёмку | `client_id` и `location_id` |

Удаление клиента каскадно удаляет его заявки (`ON DELETE CASCADE`). Локацию, на которую ссылаются заявки, база не удаляет (`ON DELETE RESTRICT`). Дополнительно сервис запрещает удалять локацию, пока на неё есть заявки в статусах `NEW`, `APPROVED` или `IN_PROGRESS`.

Статусы заявки: `NEW`, `APPROVED`, `IN_PROGRESS`, `COMPLETED`, `REJECTED`, `CANCELLED`. Типы локации: `PAVILION`, `OUTDOOR`, `STUDIO`, `OFFICE`.

## Функции меню

1. Клиенты — создание, список, поиск по id, изменение, удаление.
2. Локации — те же операции.
3. Заявки — те же операции и смена статуса.
4. Поиск — по фрагменту названия заявки и по названию локации.
5. Фильтрация — по статусу и по диапазону дат съёмки; сортировка по дате и по бюджету.
6. Статистика — число заявок по каждому статусу, суммарный бюджет, средняя длительность, число клиентов и локаций.
7. Экспорт — файл `kinostudio_export.xlsx` с тремя листами.
8. Имена таблиц базы.
0. Выход. После каждой операции программа возвращается в главное меню.

## Бизнес-правила

Правила реализованы в сервисах и не зависят от текста меню.

1. Название и описание заявки обязательны.
2. Клиент с указанным id должен существовать.
3. Локация с указанным id должна существовать.
4. Дата съёмки не может быть раньше текущего дня.
5. Длительность больше нуля, бюджет не отрицательный.
6. Переходы статуса ограничены графом: `NEW` → `APPROVED` или `REJECTED`; `APPROVED` → `IN_PROGRESS` или `CANCELLED`; `IN_PROGRESS` → `COMPLETED` или `CANCELLED`. Из `COMPLETED`, `REJECTED` и `CANCELLED` переходы запрещены. Новая заявка создаётся в статусе `NEW`.
7. У локации обязательны название и адрес, вместимость больше нуля.
8. Локацию с активными заявками удалить нельзя.
9. У клиента обязательны ФИО, телефон и почта, содержащая символ `@`.

Некорректный ввод не завершает программу: текст вместо числа, неверная дата, отсутствующая запись, нарушение правила, ошибка соединения и ошибка SQL обрабатываются отдельно.

## Запуск

### Что должно быть установлено

- JDK 17
- Apache Maven 3.8 или новее
- PostgreSQL 14 или новее
- Утилиты `createdb` и `psql` доступны в `PATH`

Проверка:

```bash
java -version
mvn -version
psql --version
```

На macOS с Homebrew JDK 17 может быть keg-only. Тогда перед сборкой:

```bash
export JAVA_HOME="$(brew --prefix openjdk@17)"
export PATH="$JAVA_HOME/bin:$(brew --prefix postgresql@15)/bin:$PATH"
```

### 1. Создать базу и загрузить схему

Пользователь и пароль в коде по умолчанию: `postgres` / `postgres`. База: `kinostudio_db`, порт `5432`.

```bash
psql -d postgres -c "CREATE ROLE postgres WITH LOGIN PASSWORD 'postgres' SUPERUSER;"
createdb -O postgres kinostudio_db
psql -d kinostudio_db -f sql/schema.sql
```

Если роль `postgres` уже есть, первую команду пропустите. Если локальная аутентификация настроена как `trust`, пароль при входе через `psql` не запрашивается, но JDBC-приложению константы пользователя и пароля всё равно нужны.

Скрипт создаёт таблицы, ограничения и тестовый набор: 5 клиентов, 8 локаций, 10 заявок. Повторный запуск скрипта удаляет эти таблицы и заполняет их заново.

Проверка загрузки:

```bash
psql -d kinostudio_db -c "SELECT COUNT(*) FROM clients; SELECT COUNT(*) FROM locations; SELECT COUNT(*) FROM shooting_requests;"
```

Ожидаемые количества: 5, 8 и 10.

### 2. Согласовать параметры подключения

Если сервер, порт, имя базы, пользователь или пароль отличаются от значений по умолчанию, измените константы в `src/main/java/ru/mirea/kinostudio/util/DatabaseManager.java`:

```java
private static final String URL = "jdbc:postgresql://localhost:5432/kinostudio_db";
private static final String USER = "postgres";
private static final String PASSWORD = "postgres";
```

### 3. Собрать исполняемый архив

Из корня репозитория:

```bash
mvn clean package
```

Maven загрузит драйвер PostgreSQL и Apache POI, скомпилирует исходники и плагином `maven-shade-plugin` соберёт jar со всеми зависимостями. Главный класс — `ru.mirea.kinostudio.Main`. Готовый файл: `target/kinostudio-crm.jar`.

### 4. Запустить программу

```bash
java -jar target/kinostudio-crm.jar
```

В консоли появится меню «Система киностудии — заявки на съёмку». Номер пункта вводится с клавиатуры. Для выхода выберите `0`.

Экспорт (пункт 7) создаёт `kinostudio_export.xlsx` в текущем каталоге. Запускайте команду из корня проекта, если файл должен лежать рядом с исходниками.

### Если программа не подключается к базе

Сообщение «Ошибка базы данных» означает, что сервер недоступен, база не создана или константы в `DatabaseManager` не совпадают с локальной установкой. Проверьте, что PostgreSQL принимает соединения:

```bash
pg_isready -h localhost -p 5432
```

Затем повторите шаг 1 и сверьте константы подключения.
