-- Запускать один раз через psql -v ON_ERROR_STOP=1 -1 -f ...
ALTER TABLE users ADD COLUMN admin BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE import_operations (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(user_id),
    status VARCHAR(16) NOT NULL CHECK (status IN ('IN_PROGRESS', 'SUCCESS', 'FAILED')),
    added_count INTEGER,
    CONSTRAINT import_operations_result CHECK (
        (status = 'SUCCESS' AND added_count IS NOT NULL AND added_count >= 0)
        OR (status IN ('IN_PROGRESS', 'FAILED') AND added_count IS NULL)
    )
);
