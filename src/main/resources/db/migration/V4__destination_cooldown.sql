ALTER TABLE destinations
    ADD COLUMN consecutive_failures INT NOT NULL DEFAULT 0,
    ADD COLUMN cooldown_until TIMESTAMPTZ;

CREATE INDEX destinations_cooldown_until_idx ON destinations (cooldown_until);
