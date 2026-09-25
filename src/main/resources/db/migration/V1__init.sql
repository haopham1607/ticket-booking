-- Placeholder table to confirm Flyway runs migrations.
CREATE TABLE app_info (
    id          BIGSERIAL PRIMARY KEY,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
