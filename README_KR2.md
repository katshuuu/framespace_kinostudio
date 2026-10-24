# Контрольная работа №2 — JavaFX-приложение «Киностудия»

Десктопное приложение поверх базы PostgreSQL из КР №1 (те же таблицы `clients`, `locations`, `shooting_requests`).

## Настройки (.env)
Все параметры подключения к БД и данные администратора хранятся в файле `.env` (в git он не попадает).
```
cp .env.example .env      # затем впишите свои значения
```
| Параметр | Назначение |
|---|---|
| `DB_HOST`, `DB_PORT`, `DB_NAME` | где находится PostgreSQL |
| `DB_USER`, `DB_PASSWORD` | логин и пароль базы |
| `ADMIN_LOGIN`, `ADMIN_PASSWORD` | единый логин и пароль администратора |
| `NOVNC_PORT`, `DB_PUBLISHED_PORT` | порты на вашем компьютере для Docker |

Приоритет: системное свойство (`-DDB_PASSWORD=...`) → переменная окружения (так передаёт Docker) → файл `.env`.

## Запуск своей КР №2 (как в задании)
По заданию Docker используется для PostgreSQL, а JavaFX-приложение запускается на компьютере как обычная Java-программа (Java 21 и Maven 3.9+).
```
cp .env.example .env          # Windows: copy .env.example .env
docker compose up -d db       # PostgreSQL с базой из КР №1 (sql/schema.sql + sql/users.sql), порт 5433 на компьютере
mvn javafx:run                # JavaFX-приложение, JDBC-подключение к этой базе
```
- В `.env` должно быть `DB_HOST=localhost` и `DB_PORT=5433` (порт, опубликованный Docker). Свою установленную PostgreSQL можно использовать, поставив `DB_PORT=5432` и свой пароль.
- Проверка базы: `docker ps` (контейнер `kinostudio-db` в состоянии healthy).
- Остановка: `docker compose down`. Данные сохраняются; полностью сбросить базу: `docker compose down -v`.
- Сборка jar: `mvn package`, запуск: `java -jar target/kinostudio-crm.jar`. Jar нужно собирать на той же ОС, где он запускается (JavaFX подбирает библиотеки под систему).
- Консольная версия из КР №1: `java -cp target/kinostudio-crm.jar ru.mirea.kinostudio.Main`.

### Дополнительно: приложение в Docker (окно в браузере)
Необязательный режим, если на компьютере нет Java/Maven: `docker compose --profile gui up --build`, затем http://localhost:6080 (кнопка «Подключиться»). Файлы экспорта попадают в папку `exports/`.

## Готовая система из архива преподавателя (для защиты)
Это отдельный проект (backend + web + Android + `javafx-client`), он нужен как пример и для проверки Docker. Порядок всегда один: **Docker → PostgreSQL и backend → проверка веб-системы → только потом JavaFX**.
1. Распаковать архив целиком в одну папку; в корне должен лежать `.env` (параметры PostgreSQL, JWT, администратор, порты).
2. Запустить Docker Desktop и дождаться состояния Running.
3. Из корня архива запустить PostgreSQL и backend:
   - Windows: `powershell -ExecutionPolicy Bypass -File .\backend\start-compose.ps1`
   - или напрямую: `docker compose --env-file .env -f backend/docker-compose.yml up -d --wait`
4. Проверка: контейнер PostgreSQL healthy (`docker ps`), контейнер backend запущен, открываются http://localhost:8080/ и http://localhost:8080/actuator/health.
5. Войти в веб-интерфейс администратором (логин и пароль из `.env` архива: `BOOTSTRAP_ADMIN_EMAIL`, `BOOTSTRAP_ADMIN_PASSWORD`).
6. Запустить JavaFX-пример отдельно от Docker: `cd javafx-client`, затем `.\run.ps1` или `mvn javafx:run` (клиент ходит в REST API http://127.0.0.1:8080/api, к PostgreSQL напрямую не подключается).
7. Остановка: `docker compose --env-file .env -f backend/docker-compose.yml down` (без `-v`, чтобы не удалить данные).
В отличие от примера, наше приложение подключается к PostgreSQL напрямую через JDBC (DAO/Repository), без REST API.

## Вход в систему
- При запуске открывается окно входа и регистрации.
- **Обычный пользователь** регистрируется сам (логин 3–30 символов, пароль от 8 символов с буквой и цифрой). Пароль хранится только в виде хеша PBKDF2 со случайной солью (таблица `users`).
- **Администратор** один: вход по `ADMIN_LOGIN` / `ADMIN_PASSWORD` из `.env`, в базе он не хранится.
- Права: пользователь — просмотр всех данных, поиск, статистика, экспорт, создание и изменение заявок. Администратор дополнительно — клиенты и локации (добавление, изменение, удаление), удаление заявок и смена статуса.
- После 5 неверных паролей подряд вход для этого логина блокируется на 30 секунд.

## Что есть в приложении
| Требование | Где реализовано |
|---|---|
| TableView, CRUD, подтверждение удаления | вкладки «Заявки», «Клиенты», «Локации» |
| Связанные данные | панель деталей заявки; у клиента и локации — таблица их заявок |
| Выбор связанных сущностей без ввода id | ComboBox клиента и локации в форме заявки |
| Валидация и понятные сообщения | `*DialogController` + `FormErrors` (красная рамка и текст), бизнес-правила КР №1 в сервисах |
| Поиск по нескольким полям | заявки: название, описание, клиент, локация; клиенты: ФИО, email, телефон |
| ≥ 2 фильтра, ≥ 2 сортировки | заявки: статус, локация, диапазон дат; 6 вариантов сортировки |
| Статистика (≥ 5 показателей) | вкладка «Статистика», `StatisticsService` (Stream API) |
| Экспорт .xlsx / .csv | кнопка «Экспорт…», `FileChooser`, `XlsxExporter`, `CsvExporter` (выгружается то, что видно в таблице) |
| FXML + CSS | `src/main/resources/fxml/*.fxml`, `css/app.css` |
| Слои | FXML → Controller (`fx/controller`) → Service → Repository → БД |
| Docker | `docker-compose.yml` (PostgreSQL); необязательно `Dockerfile` и `docker/entrypoint.sh` (приложение в браузере) |
| Вход и регистрация | `LoginController`, `AuthService`, `UserRepository`, `PasswordHasher` |
| Параметры в `.env` | `AppConfig`, `DatabaseManager` |

## Для защиты (кратко)
- **Stage / Scene / Node**: `Stage` — окно, `Scene` — содержимое окна, `Node` — любой элемент дерева (кнопка, таблица, панель). См. `FxApp`.
- **FXML ↔ Controller**: в FXML `fx:controller` указывает класс, `fx:id` связывает элемент с полем `@FXML`, `onAction="#метод"` — с обработчиком. Загрузчик — `FXMLLoader`.
- **ObservableList и TableView**: таблица подписана на список; при `setAll(...)` она перерисовывается сама. Поиск и фильтры — `FilteredList`, сортировка — `SortedList`.
- **Где логика**: правила и проверки — в `service`, SQL — только в `repository`, контроллеры лишь читают поля и показывают результат.
- **Исключения**: `BusinessException` (нарушено правило) и `EntityNotFoundException` показываются текстом; `SQLException` переводится в понятное сообщение (`DbErrors`), детали пишутся в журнал, пользователю stack trace не показывается.
- **Поиск без учёта регистра** выполняется в Java (`toLowerCase(Locale.ROOT)`), потому что у базы локаль C и `ILIKE` не различает регистр кириллицы.
- Правило КР №1 «дата съёмки не в прошлом» действует и при редактировании: заявку с прошедшей датой можно изменить, только поставив новую дату.
