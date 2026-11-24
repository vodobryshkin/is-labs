#!/bin/sh
set -eu

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
base_url=${IS_LAB1_BASE_URL:-http://localhost:8080/api/v1}

command -v psql >/dev/null
command -v hurl >/dev/null

# Удаляет данные только после проверки фактического имени БД (_test).
# API и psql должны быть подключены к одной отдельной тестовой БД с новой схемой.
psql -X -q -v ON_ERROR_STOP=1 -1 -f "$script_dir/../../db/clean-test-database.sql"
hurl --test --variable "base_url=$base_url" "$script_dir/prepare_history.hurl.in"
psql -X -q -v ON_ERROR_STOP=1 -1 -f "$script_dir/history_fixture.sql"
hurl --test --variable "base_url=$base_url" "$script_dir/history_access.hurl.in"

psql -X -q -v ON_ERROR_STOP=1 -c \
    "UPDATE users SET admin = FALSE WHERE username = 'stage02_admin'"
hurl --test --variable "base_url=$base_url" "$script_dir/revoked_admin.hurl.in"
