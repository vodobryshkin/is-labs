#!/bin/sh
set -eu

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
base_url=${IS_LAB1_BASE_URL:-http://localhost:8080/api/v1}
command -v psql >/dev/null
command -v hurl >/dev/null

# API и psql должны смотреть в одну отдельную БД с суффиксом _test.
psql -X -q -v ON_ERROR_STOP=1 -1 -f "$script_dir/../../db/clean-test-database.sql"

cleanup() {
    result=$?
    trap - 0
    if ! psql -X -q -v ON_ERROR_STOP=1 -c \
        'ALTER TABLE persons DROP CONSTRAINT IF EXISTS stage04_reject_test_weight'; then
        echo 'Не удалось удалить временный CHECK stage04_reject_test_weight.' >&2
        exit 1
    fi
    exit "$result"
}
trap cleanup 0
trap 'exit 130' 2
trap 'exit 143' 15
trap 'exit 129' 1

psql -X -q -v ON_ERROR_STOP=1 -c \
    'ALTER TABLE persons ADD CONSTRAINT stage04_reject_test_weight CHECK (weight <> 123.25)'
hurl --test --variable "base_url=$base_url" "$script_dir/file_import_storage_failure.hurl.in"
