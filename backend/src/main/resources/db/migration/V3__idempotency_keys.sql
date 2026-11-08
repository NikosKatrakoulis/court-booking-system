-- Remembers the outcome of a booking request per (user, client-supplied key).
-- Written in the SAME transaction as the booking, so either both exist or neither does.
CREATE TABLE idempotency_key (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users (id),
    key_value  VARCHAR(100) NOT NULL,
    slot_id    BIGINT       NOT NULL REFERENCES slot (id),
    booking_id BIGINT       NOT NULL REFERENCES booking (id),
    created_at TIMESTAMP    NOT NULL,
    CONSTRAINT uk_idempotency_user_key UNIQUE (user_id, key_value)
);
CREATE INDEX idx_idempotency_created ON idempotency_key (created_at);
