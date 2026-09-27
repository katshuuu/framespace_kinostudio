-- ============================================================
-- Схема БД: Киностудия — Заявки на съёмку
-- СУБД: PostgreSQL
-- ============================================================

DROP TABLE IF EXISTS shooting_requests;
DROP TABLE IF EXISTS clients;
DROP TABLE IF EXISTS locations;

-- Таблица клиентов (пользователей системы)
CREATE TABLE clients (
    id          SERIAL PRIMARY KEY,
    full_name   VARCHAR(150) NOT NULL,
    email       VARCHAR(150) NOT NULL UNIQUE,
    phone       VARCHAR(30)  NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Таблица локаций (павильоны и площадки киностудии)
CREATE TABLE locations (
    id          SERIAL PRIMARY KEY,
    name        VARCHAR(150) NOT NULL UNIQUE,
    address     VARCHAR(250) NOT NULL,
    capacity    INTEGER NOT NULL CHECK (capacity > 0),
    type        VARCHAR(20) NOT NULL DEFAULT 'PAVILION'
                CHECK (type IN ('PAVILION','OUTDOOR','STUDIO','OFFICE')),
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Таблица заявок на съёмку (основная сущность)
CREATE TABLE shooting_requests (
    id              SERIAL PRIMARY KEY,
    client_id       INTEGER NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
    location_id     INTEGER NOT NULL REFERENCES locations(id) ON DELETE RESTRICT,
    title           VARCHAR(200) NOT NULL,
    description     TEXT NOT NULL,
    shooting_date   DATE NOT NULL,
    duration_hours  INTEGER NOT NULL CHECK (duration_hours > 0),
    budget          NUMERIC(12,2) NOT NULL CHECK (budget >= 0),
    status          VARCHAR(20) NOT NULL DEFAULT 'NEW'
                    CHECK (status IN ('NEW','APPROVED','IN_PROGRESS','COMPLETED','REJECTED','CANCELLED')),
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_requests_status ON shooting_requests(status);
CREATE INDEX idx_requests_date ON shooting_requests(shooting_date);
CREATE INDEX idx_requests_location ON shooting_requests(location_id);

-- ============================================================
-- Тестовые данные
-- ============================================================

INSERT INTO clients (full_name, email, phone) VALUES
('Иван Петров',       'ivan.petrov@mail.ru',    '+7 900 111-22-33'),
('Анна Смирнова',     'anna.smirnova@mail.ru',  '+7 900 222-33-44'),
('ООО "Медиа Групп"', 'contact@mediagroup.ru',  '+7 495 333-44-55'),
('Сергей Иванов',     'sergey.ivanov@mail.ru',  '+7 900 444-55-66'),
('Ольга Кузнецова',   'olga.kuz@mail.ru',       '+7 900 555-66-77');

INSERT INTO locations (name, address, capacity, type) VALUES
('Павильон №1',      'ул. Киностудийная, д. 1',       300, 'PAVILION'),
('Павильон №2',      'ул. Киностудийная, д. 1',       450, 'PAVILION'),
('Павильон №3',      'ул. Киностудийная, д. 2',       600, 'PAVILION'),
('Студия №1',        'ул. Съёмочная, д. 5',           80,  'STUDIO'),
('Студия №2',        'ул. Съёмочная, д. 5',           60,  'STUDIO'),
('Набережная, СПб',  'Университетская набережная',    1000,'OUTDOOR'),
('Загородный клуб',  'Московская область, д. Липки',  200, 'OUTDOOR'),
('Конференц-зал',    'пр. Мира, д. 10',                50, 'OFFICE');

INSERT INTO shooting_requests (client_id, location_id, title, description, shooting_date, duration_hours, budget, status) VALUES
(1, 1, 'Реклама автомобиля',      'Съёмка рекламного ролика для нового седана', '2026-10-15', 8,  350000.00, 'NEW'),
(2, 6, 'Клип для группы "Волна"', 'Музыкальный клип, натурные съёмки',          '2026-10-20', 12, 500000.00, 'APPROVED'),
(3, 8, 'Корпоративный фильм',     'Имиджевый фильм о компании',                 '2026-09-25', 6,  200000.00, 'IN_PROGRESS'),
(1, 5, 'Съёмка интервью',         'Интервью с экспертом отрасли',               '2026-09-10', 3,  80000.00,  'COMPLETED'),
(4, 3, 'Пилотная серия',          'Пилот телесериала, первая серия',            '2026-11-05', 16, 1200000.00,'NEW'),
(5, 7, 'Свадебная съёмка',        'Полный день съёмки свадебного торжества',    '2026-10-01', 10, 150000.00, 'APPROVED'),
(2, 6, 'Документальный фильм',    'Съёмки документального фильма о городе',     '2026-12-01', 20, 600000.00, 'NEW'),
(3, 4, 'Съёмка каталога',         'Фотосъёмка каталога продукции',              '2026-09-05', 5,  90000.00,  'REJECTED'),
(4, 2, 'Тизер фильма',            'Короткий тизер для промо-кампании',          '2026-09-28', 4,  120000.00, 'IN_PROGRESS'),
(5, 8, 'Юбилейное видео',         'Съёмка видео к юбилею компании',             '2026-08-15', 7,  110000.00, 'CANCELLED');
