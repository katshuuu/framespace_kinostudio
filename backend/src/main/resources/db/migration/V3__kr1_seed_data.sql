-- Данные из sql/schema.sql КР №1 (ветка main): клиенты, локации и заявки на съёмку.
-- Телефоны приведены к формату +7-XXX-XXX-XX-XX, у локаций появились коды,
-- у заявок — externalId KR1-NNN. ON CONFLICT не даёт затереть данные, созданные вручную.

INSERT INTO client (id, full_name, email, phone, created_at, updated_at)
VALUES (gen_random_uuid(), 'Иван Петров',       'ivan.petrov@mail.ru',   '+7-900-111-22-33', NOW(), NOW()),
       (gen_random_uuid(), 'Анна Смирнова',     'anna.smirnova@mail.ru', '+7-900-222-33-44', NOW(), NOW()),
       (gen_random_uuid(), 'ООО "Медиа Групп"', 'contact@mediagroup.ru', '+7-495-333-44-55', NOW(), NOW()),
       (gen_random_uuid(), 'Сергей Иванов',     'sergey.ivanov@mail.ru', '+7-900-444-55-66', NOW(), NOW()),
       (gen_random_uuid(), 'Ольга Кузнецова',   'olga.kuz@mail.ru',      '+7-900-555-66-77', NOW(), NOW())
ON CONFLICT (email) DO NOTHING;

INSERT INTO location (id, code, name, address, capacity, type, active, created_at, updated_at)
VALUES (gen_random_uuid(), 'PAV-1',     'Павильон №1',     'ул. Киностудийная, д. 1',      300,  'PAVILION', TRUE, NOW(), NOW()),
       (gen_random_uuid(), 'PAV-2',     'Павильон №2',     'ул. Киностудийная, д. 1',      450,  'PAVILION', TRUE, NOW(), NOW()),
       (gen_random_uuid(), 'PAV-3',     'Павильон №3',     'ул. Киностудийная, д. 2',      600,  'PAVILION', TRUE, NOW(), NOW()),
       (gen_random_uuid(), 'STUDIO-1',  'Студия №1',       'ул. Съёмочная, д. 5',          80,   'STUDIO',   TRUE, NOW(), NOW()),
       (gen_random_uuid(), 'STUDIO-2',  'Студия №2',       'ул. Съёмочная, д. 5',          60,   'STUDIO',   TRUE, NOW(), NOW()),
       (gen_random_uuid(), 'SPB-EMB',   'Набережная, СПб', 'Университетская набережная',   1000, 'OUTDOOR',  TRUE, NOW(), NOW()),
       (gen_random_uuid(), 'CLUB',      'Загородный клуб', 'Московская область, д. Липки', 200,  'OUTDOOR',  TRUE, NOW(), NOW()),
       (gen_random_uuid(), 'CONF-HALL', 'Конференц-зал',   'пр. Мира, д. 10',              50,   'OFFICE',   TRUE, NOW(), NOW())
ON CONFLICT (code) DO NOTHING;

INSERT INTO shooting_request (id, external_id, title, description, client_id, location_id, shooting_date,
                              duration_hours, budget, status, urgent, priority_level, archived,
                              created_at, updated_at)
SELECT gen_random_uuid(), s.external_id, s.title, s.description, c.id, l.id, s.shooting_date,
       s.duration_hours, s.budget, s.status, FALSE, s.priority_level, FALSE, NOW(), NOW()
FROM (VALUES
          ('KR1-001', 'Реклама автомобиля',      'Съёмка рекламного ролика для нового седана', 'ivan.petrov@mail.ru',   'PAV-1',     DATE '2026-10-15', 8,  350000.00,  'NEW',         'MEDIUM'),
          ('KR1-002', 'Клип для группы "Волна"', 'Музыкальный клип, натурные съёмки',          'anna.smirnova@mail.ru', 'SPB-EMB',   DATE '2026-10-20', 12, 500000.00,  'APPROVED',    'HIGH'),
          ('KR1-003', 'Корпоративный фильм',     'Имиджевый фильм о компании',                 'contact@mediagroup.ru', 'CONF-HALL', DATE '2026-09-25', 6,  200000.00,  'IN_PROGRESS', 'MEDIUM'),
          ('KR1-004', 'Съёмка интервью',         'Интервью с экспертом отрасли',               'ivan.petrov@mail.ru',   'STUDIO-2',  DATE '2026-09-10', 3,  80000.00,   'COMPLETED',   'NOT_APPLICABLE'),
          ('KR1-005', 'Пилотная серия',          'Пилот телесериала, первая серия',            'sergey.ivanov@mail.ru', 'PAV-3',     DATE '2026-11-05', 16, 1200000.00, 'NEW',         'HIGH'),
          ('KR1-006', 'Свадебная съёмка',        'Полный день съёмки свадебного торжества',    'olga.kuz@mail.ru',      'CLUB',      DATE '2026-10-01', 10, 150000.00,  'APPROVED',    'MEDIUM'),
          ('KR1-007', 'Документальный фильм',    'Съёмки документального фильма о городе',     'anna.smirnova@mail.ru', 'SPB-EMB',   DATE '2026-12-01', 20, 600000.00,  'NEW',         'HIGH'),
          ('KR1-008', 'Съёмка каталога',         'Фотосъёмка каталога продукции',              'contact@mediagroup.ru', 'STUDIO-1',  DATE '2026-09-05', 5,  90000.00,   'REJECTED',    'NOT_APPLICABLE'),
          ('KR1-009', 'Тизер фильма',            'Короткий тизер для промо-кампании',          'sergey.ivanov@mail.ru', 'PAV-2',     DATE '2026-09-28', 4,  120000.00,  'IN_PROGRESS', 'LOW'),
          ('KR1-010', 'Юбилейное видео',         'Съёмка видео к юбилею компании',             'olga.kuz@mail.ru',      'CONF-HALL', DATE '2026-08-15', 7,  110000.00,  'CANCELLED',   'NOT_APPLICABLE')
     ) AS s(external_id, title, description, client_email, location_code, shooting_date,
            duration_hours, budget, status, priority_level)
         JOIN client c ON c.email = s.client_email
         JOIN location l ON l.code = s.location_code
ON CONFLICT (external_id) DO NOTHING;
