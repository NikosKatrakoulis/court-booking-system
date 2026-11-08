-- Core schema. Must match the JPA entities (ddl-auto=validate checks this on startup).

CREATE TABLE court_type (
    id               BIGSERIAL PRIMARY KEY,
    name             VARCHAR(255)  NOT NULL UNIQUE,
    duration_minutes INTEGER       NOT NULL,
    price_per_slot   NUMERIC(19,2) NOT NULL,
    is_active        BOOLEAN       NOT NULL DEFAULT TRUE
);

CREATE TABLE court (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(255) NOT NULL UNIQUE,
    court_type_id BIGINT       NOT NULL REFERENCES court_type (id),
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE slot (
    id         BIGSERIAL PRIMARY KEY,
    court_id   BIGINT NOT NULL REFERENCES court (id),
    date       DATE   NOT NULL,
    start_time TIME   NOT NULL,
    end_time   TIME   NOT NULL,
    CONSTRAINT uk_slot_court_date_start UNIQUE (court_id, date, start_time)
);

CREATE TABLE users (
    id         BIGSERIAL PRIMARY KEY,
    email      VARCHAR(255) NOT NULL UNIQUE,
    first_name VARCHAR(255) NOT NULL,
    last_name  VARCHAR(255) NOT NULL,
    password   VARCHAR(255) NOT NULL,
    role       VARCHAR(255) NOT NULL,
    created_at TIMESTAMP    NOT NULL
);

CREATE TABLE booking (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT       NOT NULL REFERENCES users (id),
    slot_id      BIGINT       NOT NULL REFERENCES slot (id),
    status       VARCHAR(255) NOT NULL,
    created_at   TIMESTAMP    NOT NULL,
    cancelled_at TIMESTAMP
);

-- Defense in depth for double-booking: the DB itself refuses a second CONFIRMED booking
-- for the same slot, even if application-level locking were bypassed.
CREATE UNIQUE INDEX uk_booking_slot_confirmed ON booking (slot_id) WHERE status = 'CONFIRMED';
CREATE INDEX idx_booking_user ON booking (user_id);

CREATE TABLE refresh_token (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users (id),
    token      VARCHAR(255) NOT NULL UNIQUE,
    is_revoked BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP    NOT NULL,
    expires_at TIMESTAMP    NOT NULL
);
