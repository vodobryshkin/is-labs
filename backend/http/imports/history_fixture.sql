-- После prepare_history.hurl.in; только отдельная тестовая БД.
DO $$
BEGIN
    IF current_database() !~ '_test$' THEN
        RAISE EXCEPTION 'History fixture requires a separate database ending in _test';
    END IF;
    IF (SELECT COUNT(*) FROM users
        WHERE username IN ('stage02_alice', 'stage02_bob', 'stage02_admin')) <> 3 THEN
        RAISE EXCEPTION 'Run prepare_history.hurl.in first';
    END IF;
    IF EXISTS (SELECT 1 FROM users WHERE admin) THEN
        RAISE EXCEPTION 'Registration must not grant administrator privileges';
    END IF;
END
$$;

UPDATE users SET admin = TRUE WHERE username = 'stage02_admin';

INSERT INTO import_operations (user_id, status, added_count)
VALUES
    ((SELECT user_id FROM users WHERE username = 'stage02_alice'), 'SUCCESS', 3),
    ((SELECT user_id FROM users WHERE username = 'stage02_alice'), 'IN_PROGRESS', NULL),
    ((SELECT user_id FROM users WHERE username = 'stage02_alice'), 'FAILED', NULL),
    ((SELECT user_id FROM users WHERE username = 'stage02_bob'), 'SUCCESS', 2),
    ((SELECT user_id FROM users WHERE username = 'stage02_admin'), 'FAILED', NULL);

-- Недопустимые сочетания статуса и счётчика должны отклоняться самой БД.
DO $$
DECLARE
    owner_id BIGINT := (SELECT user_id FROM users WHERE username = 'stage02_alice');
BEGIN
    BEGIN
        INSERT INTO import_operations (user_id, status, added_count)
        VALUES (owner_id, 'FAILED', 1);
        RAISE EXCEPTION 'FAILED with a count was accepted';
    EXCEPTION WHEN check_violation THEN NULL;
    END;
    BEGIN
        INSERT INTO import_operations (user_id, status, added_count)
        VALUES (owner_id, 'SUCCESS', NULL);
        RAISE EXCEPTION 'SUCCESS without a count was accepted';
    EXCEPTION WHEN check_violation THEN NULL;
    END;
    BEGIN
        INSERT INTO import_operations (user_id, status, added_count)
        VALUES (owner_id, 'SUCCESS', -1);
        RAISE EXCEPTION 'Negative count was accepted';
    EXCEPTION WHEN check_violation THEN NULL;
    END;
END
$$;
