-- Приведение схемы к модели КР №1: отдельная таблица клиентов и тип локации.

CREATE TABLE client (
                        id UUID PRIMARY KEY,
                        full_name VARCHAR(150) NOT NULL,
                        email VARCHAR(254) NOT NULL UNIQUE,
                        phone VARCHAR(32) NOT NULL,
                        created_at TIMESTAMPTZ NOT NULL,
                        updated_at TIMESTAMPTZ NOT NULL,
                        version BIGINT NOT NULL DEFAULT 0,

                        CONSTRAINT chk_client_email_lower CHECK (email = LOWER(email))
);

CREATE INDEX idx_client_full_name
    ON client(full_name);


-- Перенос клиентов из уже существующих заявок (если база была создана до V2).
-- Заявки без email получают технический адрес, чтобы не потерять связь.
-- Имя берётся из последней заявки клиента, телефон — из последней заявки, где он указан.
CREATE TEMPORARY TABLE tmp_request_client AS
SELECT r.id AS request_id,
       COALESCE(NULLIF(LOWER(TRIM(r.client_email)), ''),
                'no-email-' || r.id::text || '@framespace.local') AS email,
       LEFT(r.client_name, 150) AS full_name,
       NULLIF(TRIM(r.client_phone), '') AS phone,
       r.updated_at
FROM shooting_request r;

INSERT INTO client (id, full_name, email, phone, created_at, updated_at)
SELECT gen_random_uuid(), latest.full_name, latest.email,
       COALESCE((SELECT t.phone
                 FROM tmp_request_client t
                 WHERE t.email = latest.email AND t.phone IS NOT NULL
                 ORDER BY t.updated_at DESC
                 LIMIT 1), '+7-000-000-00-00'),
       NOW(), NOW()
FROM (
         SELECT DISTINCT ON (email) email, full_name
         FROM tmp_request_client
         ORDER BY email, updated_at DESC
     ) latest;

ALTER TABLE shooting_request
    ADD COLUMN client_id UUID REFERENCES client(id) ON DELETE RESTRICT;

UPDATE shooting_request r
SET client_id = c.id
FROM tmp_request_client t
         JOIN client c ON c.email = t.email
WHERE t.request_id = r.id;

DROP TABLE tmp_request_client;

ALTER TABLE shooting_request
    ALTER COLUMN client_id SET NOT NULL;

ALTER TABLE shooting_request
    DROP COLUMN client_name,
    DROP COLUMN client_email,
    DROP COLUMN client_phone;

CREATE INDEX idx_shooting_request_client
    ON shooting_request(client_id);

CREATE INDEX idx_shooting_request_date
    ON shooting_request(shooting_date);


ALTER TABLE location
    ADD COLUMN type VARCHAR(20) NOT NULL DEFAULT 'PAVILION';

ALTER TABLE location
    ADD CONSTRAINT chk_location_type
        CHECK (type IN ('PAVILION', 'OUTDOOR', 'STUDIO', 'OFFICE'));

ALTER TABLE location
    ADD CONSTRAINT chk_location_capacity
        CHECK (capacity IS NULL OR capacity > 0);


ALTER TABLE shooting_request
    ADD CONSTRAINT chk_shooting_request_status
        CHECK (status IN ('NEW', 'APPROVED', 'IN_PROGRESS', 'COMPLETED', 'REJECTED', 'CANCELLED'));
