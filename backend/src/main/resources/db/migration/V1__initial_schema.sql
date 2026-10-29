CREATE TABLE app_user (
                          id UUID PRIMARY KEY,
                          email VARCHAR(254) NOT NULL UNIQUE,
                          display_name VARCHAR(160) NOT NULL,
                          password_hash VARCHAR(100) NOT NULL,
                          enabled BOOLEAN NOT NULL DEFAULT TRUE,
                          password_change_required BOOLEAN NOT NULL DEFAULT TRUE,
                          failed_login_attempts INTEGER NOT NULL DEFAULT 0,
                          locked_until TIMESTAMPTZ,
                          created_at TIMESTAMPTZ NOT NULL,
                          updated_at TIMESTAMPTZ NOT NULL,
                          version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE app_user_role (
                               user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
                               role VARCHAR(32) NOT NULL,
                               PRIMARY KEY (user_id, role)
);

CREATE TABLE refresh_session (
                                 id UUID PRIMARY KEY,
                                 user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
                                 token_hash VARCHAR(64) NOT NULL UNIQUE,
                                 expires_at TIMESTAMPTZ NOT NULL,
                                 revoked_at TIMESTAMPTZ,
                                 replaced_by UUID,
                                 user_agent VARCHAR(500),
                                 ip_address VARCHAR(64),
                                 created_at TIMESTAMPTZ NOT NULL,
                                 last_used_at TIMESTAMPTZ
);

CREATE INDEX idx_refresh_user
    ON refresh_session(user_id);

CREATE INDEX idx_refresh_expires
    ON refresh_session(expires_at);


CREATE TABLE location (
                          id UUID PRIMARY KEY,
                          code VARCHAR(50) NOT NULL UNIQUE,
                          name VARCHAR(200),
                          address VARCHAR(500),
                          capacity INTEGER,
                          active BOOLEAN NOT NULL DEFAULT TRUE,
                          created_at TIMESTAMPTZ NOT NULL,
                          updated_at TIMESTAMPTZ NOT NULL,
                          version BIGINT NOT NULL DEFAULT 0
);


CREATE TABLE shooting_request (
                                  id UUID PRIMARY KEY,
                                  external_id VARCHAR(100) UNIQUE,
                                  title VARCHAR(200) NOT NULL,
                                  description VARCHAR(4000) NOT NULL,
                                  client_name VARCHAR(200) NOT NULL,
                                  client_email VARCHAR(254),
                                  client_phone VARCHAR(32),
                                  shooting_date DATE NOT NULL,
                                  location_id UUID REFERENCES location(id),
                                  status VARCHAR(32) NOT NULL,
                                  budget NUMERIC(10,2),
                                  duration_hours INTEGER,
                                  urgent BOOLEAN NOT NULL DEFAULT FALSE,
                                  priority_level VARCHAR(32) NOT NULL,
                                  archived BOOLEAN NOT NULL DEFAULT FALSE,
                                  archived_at TIMESTAMPTZ,
                                  archived_by UUID REFERENCES app_user(id),
                                  created_by UUID REFERENCES app_user(id),
                                  updated_by UUID REFERENCES app_user(id),
                                  created_at TIMESTAMPTZ NOT NULL,
                                  updated_at TIMESTAMPTZ NOT NULL,
                                  version BIGINT NOT NULL DEFAULT 0,

                                  CONSTRAINT chk_shooting_request_budget
                                      CHECK (
                                          budget IS NULL
                                              OR (budget >= 0.00 AND budget <= 99999999.99)
                                          ),
                                  CONSTRAINT chk_shooting_request_duration
                                      CHECK (
                                          duration_hours IS NULL
                                              OR duration_hours > 0
                                          )
);

COMMENT ON COLUMN shooting_request.budget
    IS 'Бюджет заявки в рублях (0..99999999.99)';

CREATE INDEX idx_shooting_request_title
    ON shooting_request(title);

CREATE INDEX idx_shooting_request_location
    ON shooting_request(location_id);

CREATE INDEX idx_shooting_request_status
    ON shooting_request(status);

CREATE INDEX idx_shooting_request_archived
    ON shooting_request(archived);

CREATE INDEX idx_shooting_request_priority
    ON shooting_request(priority_level);


CREATE TABLE request_tag (
                             request_id UUID NOT NULL REFERENCES shooting_request(id) ON DELETE CASCADE,
                             tag VARCHAR(50) NOT NULL,
                             PRIMARY KEY (request_id, tag)
);

CREATE INDEX idx_request_tag_value
    ON request_tag(tag);


CREATE TABLE request_note (
                              id UUID PRIMARY KEY,
                              request_id UUID NOT NULL REFERENCES shooting_request(id) ON DELETE CASCADE,
                              author_id UUID NOT NULL REFERENCES app_user(id),
                              body VARCHAR(4000) NOT NULL,
                              pinned BOOLEAN NOT NULL DEFAULT FALSE,
                              created_at TIMESTAMPTZ NOT NULL,
                              updated_at TIMESTAMPTZ NOT NULL,
                              version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_note_request
    ON request_note(request_id, pinned DESC, created_at DESC);


CREATE TABLE audit_event (
                             id UUID PRIMARY KEY,
                             actor_id UUID,
                             actor_email VARCHAR(254),
                             event_type VARCHAR(80) NOT NULL,
                             entity_type VARCHAR(80),
                             entity_id UUID,
                             summary VARCHAR(1000) NOT NULL,
                             correlation_id VARCHAR(100),
                             ip_address VARCHAR(64),
                             user_agent VARCHAR(500),
                             occurred_at TIMESTAMPTZ NOT NULL,
                             details_json TEXT
);

CREATE INDEX idx_audit_occurred
    ON audit_event(occurred_at DESC);

CREATE INDEX idx_audit_actor
    ON audit_event(actor_email);

CREATE INDEX idx_audit_entity
    ON audit_event(entity_type, entity_id);
