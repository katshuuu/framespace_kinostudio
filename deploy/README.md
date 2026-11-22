# КР №3 СПО — развёртывание FrameSpace

Ветка: **`kr3-SPO`**.  
Задание: выбор инфраструктуры, настройка сервера, деплой, безопасность и мониторинг; отчёт о тестах работоспособности, нагрузки и безопасности.

## 1. Выбор серверной инфраструктуры

Выбрана контейнерная инфраструктура на базе **Docker Compose**:

| Компонент | Технология | Назначение |
|---|---|---|
| Приложение | Spring Boot 3 / Java 21 (контейнер Maven Temurin) | REST API + web UI |
| СУБД | PostgreSQL 17 | постоянные данные |
| Reverse proxy | nginx 1.27 | TLS, заголовки безопасности, rate limit |
| Мониторинг | health-monitor + Spring Actuator | опрос `/actuator/health` |

Почему контейнеры, а не «голая» ВМ с ручной установкой JDK:

- воспроизводимый стенд для преподавателя (`git clone` → один скрипт);
- изоляция процессов, `cap_drop`, `no-new-privileges`;
- порты БД и приложения привязаны к `127.0.0.1`;
- тот же стек переносится на VPS/облако без смены архитектуры.

Альтернативы (облачный PaaS, Kubernetes) для учебной КР избыточны: достаточно Compose на локальной машине или одной VPS.

## 2. Настройка сервера

### Локальный учебный стенд (macOS / Docker Desktop)

1. Установить Docker Desktop.
2. Создать рабочий каталог и клонировать репозиторий.
3. Пользователь ОС — обычный пользователь (не root); Docker Desktop работает от его имени.
4. Секреты только в `.env` (не в git).

```bash
git clone https://github.com/katshuuu/framespace_kinostudio.git
cd framespace_kinostudio
git checkout kr3-SPO
cp .env.example .env
```

### Linux VPS (боевой контур)

1. Обновить ОС: `apt update && apt upgrade -y`.
2. Создать пользователя `framespace` с группой `docker`, без парольного root-SSH.
3. Установить Docker Engine + Compose plugin.
4. Применить брандмауэр: `sudo ./deploy/scripts/firewall-ufw.sh`  
   (открыты 22/80/443; PostgreSQL и `:8080` снаружи закрыты).

## 3. Деплой приложения

```bash
./deploy/scripts/deploy-spo.sh
```

Скрипт:

1. создаёт `.env` при необходимости;
2. генерирует самоподписанный TLS (`deploy/scripts/generate-ssl.sh`);
3. поднимает `backend/docker-compose.yml` + `deploy/docker-compose.spo.yml`.

Адреса после старта:

| Назначение | URL |
|---|---|
| Прямой HTTP (внутренний) | http://localhost:8080/ |
| HTTPS через nginx | https://localhost:8443/ |
| Health | http://localhost:8080/actuator/health |
| Админ | `admin@example.org` / пароль из `.env` |

## 4. Безопасность и мониторинг

**Безопасность**

- TLS 1.2/1.3 на nginx, HSTS, `X-Frame-Options`, `nosniff`, CSP;
- JWT + роли VIEWER/OPERATOR/ADMIN, Swagger только ADMIN;
- Actuator: публичен только `health`/`info`;
- PostgreSQL на `127.0.0.1`;
- контейнеры: `cap_drop: ALL`, `no-new-privileges`;
- политика паролей, блокировка попыток входа;
- UFW-скрипт для VPS.

**Мониторинг**

- Docker healthcheck backend/db/proxy;
- контейнер `health-monitor` пишет лог опросов health;
- `./deploy/scripts/monitor-health.sh` — снимок состояния;
- метрики нагрузки — результаты ApacheBench в `deploy/results/`.

## 5. Прогон тестов для отчёта

```bash
./deploy/scripts/functional-tests.sh
./deploy/scripts/load-tests.sh
./deploy/scripts/security-tests.sh
./deploy/scripts/monitor-health.sh
```

Артефакты: `deploy/results/`.  
Итоговый PDF: `docs/kr3-spo/report-kr3-spo.pdf`.
