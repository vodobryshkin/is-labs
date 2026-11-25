#!/bin/sh
set -eu

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
base_url=${IS_LAB1_BASE_URL:-http://localhost:8080/api/v1}

command -v psql >/dev/null
command -v hurl >/dev/null

psql -X -q -v ON_ERROR_STOP=1 -1 -f "$script_dir/../../db/clean-test-database.sql"

hurl --test --variable "base_url=$base_url" "$script_dir/wrong_extension.hurl"