CREATE TABLE destinations (
    id UUID PRIMARY KEY,
    name TEXT NOT NULL UNIQUE,
    url TEXT NOT NULL,
    auth_type TEXT,
    auth_secret TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE events (
    id UUID PRIMARY KEY,
    destination_id UUID NOT NULL REFERENCES destinations(id),
    idempotency_key TEXT,
    payload JSONB NOT NULL,
    received_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    request_id TEXT,
    source_ip TEXT,
    user_agent TEXT
);

CREATE TABLE deliveries (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL REFERENCES events(id),
    destination_id UUID NOT NULL REFERENCES destinations(id),
    status TEXT NOT NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_attempt_at TIMESTAMPTZ,
    last_status_code INT,
    last_error TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT deliveries_status_check CHECK (status IN ('PENDING', 'IN_PROGRESS', 'DELIVERED', 'FAILED'))
);

CREATE INDEX deliveries_status_next_attempt_idx
    ON deliveries (status, next_attempt_at);

CREATE INDEX deliveries_destination_status_idx
    ON deliveries (destination_id, status);
