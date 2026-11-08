CREATE TABLE waitlist_entry (
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT       NOT NULL REFERENCES users (id),
    slot_id          BIGINT       NOT NULL REFERENCES slot (id),
    status           VARCHAR(255) NOT NULL,
    created_at       TIMESTAMP    NOT NULL,
    offer_expires_at TIMESTAMP
);

-- A user can be actively queued for a slot only once (history rows are allowed).
CREATE UNIQUE INDEX uk_waitlist_active_user_slot
    ON waitlist_entry (user_id, slot_id) WHERE status IN ('WAITING', 'OFFERED');
-- At most one outstanding offer per slot.
CREATE UNIQUE INDEX uk_waitlist_one_offer_per_slot
    ON waitlist_entry (slot_id) WHERE status = 'OFFERED';
CREATE INDEX idx_waitlist_slot_status_created ON waitlist_entry (slot_id, status, created_at);

CREATE TABLE notification (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users (id),
    message    VARCHAR(500) NOT NULL,
    created_at TIMESTAMP    NOT NULL
);
CREATE INDEX idx_notification_user_created ON notification (user_id, created_at DESC);
