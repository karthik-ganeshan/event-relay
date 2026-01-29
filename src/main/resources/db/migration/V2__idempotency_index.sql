CREATE UNIQUE INDEX events_destination_idempotency_key_idx
    ON events (destination_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;
