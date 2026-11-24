#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")"

DB_CONTAINER=is-lab1-postgres
BACKEND_CONTAINER=is-lab1-backend
NETWORK=is-lab1-network
VOLUME=is-lab1-postgres-data
DB_USER=islab1
DB_NAME=islab1
DB_PASSWORD="${IS_LAB1_DB_PASSWORD:-dev-password}"

for command in docker npm; do
  if ! command -v "$command" >/dev/null 2>&1; then
    echo "Missing required command: $command" >&2
    exit 1
  fi
done

docker info >/dev/null 2>&1 || { echo 'Docker daemon is not available' >&2; exit 1; }
docker network inspect "$NETWORK" >/dev/null 2>&1 || docker network create "$NETWORK" >/dev/null
docker volume inspect "$VOLUME" >/dev/null 2>&1 || docker volume create "$VOLUME" >/dev/null

if docker container inspect "$DB_CONTAINER" >/dev/null 2>&1; then
  if [[ "$(docker inspect -f '{{.State.Running}}' "$DB_CONTAINER")" != true ]]; then
    docker start "$DB_CONTAINER" >/dev/null
  fi
else
  docker run -d \
    --name "$DB_CONTAINER" \
    --network "$NETWORK" \
    -e POSTGRES_USER="$DB_USER" \
    -e POSTGRES_PASSWORD="$DB_PASSWORD" \
    -e POSTGRES_DB="$DB_NAME" \
    -v "$VOLUME:/var/lib/postgresql/data" \
    postgres:17 >/dev/null
fi

echo 'Waiting for PostgreSQL...'
ready=0
for i in {1..60}; do
  if docker exec "$DB_CONTAINER" pg_isready -U "$DB_USER" -d "$DB_NAME" >/dev/null 2>&1; then
    ready=1
    break
  fi
  sleep 1
done
if [[ "$ready" -ne 1 ]]; then
  echo 'PostgreSQL did not become ready' >&2
  docker logs --tail 60 "$DB_CONTAINER" >&2
  exit 1
fi

# schema.sql uses CREATE TABLE IF NOT EXISTS; it does not erase existing data.
docker exec -i "$DB_CONTAINER" \
  psql -U "$DB_USER" -d "$DB_NAME" -v ON_ERROR_STOP=1 -1 \
  < backend/db/schema.sql

echo 'Building backend with its Dockerfile...'
docker build -t is-lab1-backend ./backend

if docker container inspect "$BACKEND_CONTAINER" >/dev/null 2>&1; then
  docker rm -f "$BACKEND_CONTAINER" >/dev/null
fi

docker run -d \
  --name "$BACKEND_CONTAINER" \
  --network "$NETWORK" \
  -p 8080:8080 \
  -e IS_LAB1_DB_URL="jdbc:postgresql://${DB_CONTAINER}:5432/${DB_NAME}" \
  -e IS_LAB1_DB_USER="$DB_USER" \
  -e IS_LAB1_DB_PASSWORD="$DB_PASSWORD" \
  is-lab1-backend >/dev/null

echo 'Backend: http://localhost:8080/api/v1'
echo 'Frontend: http://localhost:5173'
echo 'Starting frontend...'
cd frontend
npm ci
npm run dev