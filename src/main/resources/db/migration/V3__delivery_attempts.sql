CREATE TABLE delivery_attempts (
    id UUID PRIMARY KEY,
    delivery_id UUID NOT NULL REFERENCES deliveries(id) ON DELETE CASCADE,
    attempt_no INT NOT NULL,
    status_code INT,
    error TEXT,
    started_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    finished_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX delivery_attempts_delivery_id_idx ON delivery_attempts (delivery_id);
CREATE INDEX delivery_attempts_delivery_id_attempt_no_idx ON delivery_attempts (delivery_id, attempt_no);
