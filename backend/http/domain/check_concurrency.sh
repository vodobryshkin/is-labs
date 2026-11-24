#!/bin/sh
set -eu

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
command -v psql >/dev/null
command -v python3 >/dev/null

# API должен использовать ту же отдельную БД с суффиксом _test.
psql -X -q -v ON_ERROR_STOP=1 -1 -f "$script_dir/../../db/clean-test-database.sql"
python3 "$script_dir/concurrent_rules.py"
