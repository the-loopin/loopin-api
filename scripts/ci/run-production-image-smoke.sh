#!/usr/bin/env bash

set -Eeuo pipefail

readonly compose_file="docker-compose.smoke.yml"
readonly compose_project="${SMOKE_COMPOSE_PROJECT:-loopin-production-smoke}"
readonly dependency_timeout_seconds="${SMOKE_DEPENDENCY_TIMEOUT_SECONDS:-60}"
readonly readiness_timeout_seconds="${SMOKE_READINESS_TIMEOUT_SECONDS:-120}"
readonly readiness_poll_seconds="${SMOKE_READINESS_POLL_SECONDS:-2}"
readonly api_image="${SMOKE_API_IMAGE:-loopin-api:ci}"

compose() {
  docker compose --project-name "$compose_project" --file "$compose_file" "$@"
}

if [[ -z "${SMOKE_API_PORT:-}" ]]; then
  SMOKE_API_PORT="$(python3 -c 'import socket; s = socket.socket(); s.bind(("127.0.0.1", 0)); print(s.getsockname()[1]); s.close()')"
  export SMOKE_API_PORT
fi

readonly readiness_url="http://127.0.0.1:${SMOKE_API_PORT}/api/actuator/health/readiness"
readonly events_url="http://127.0.0.1:${SMOKE_API_PORT}/api/v1/events?page=0&size=1"

if ! docker image inspect "$api_image" >/dev/null 2>&1; then
  echo "Required production image '$api_image' is not available locally." >&2
  exit 1
fi

echo "Starting production-image smoke dependencies with project '$compose_project'."
compose up --detach --wait --wait-timeout "$dependency_timeout_seconds" postgres redis

echo "Starting the already-built production image '$api_image' on localhost:$SMOKE_API_PORT."
compose up --detach api

readiness_response="$(mktemp)"
events_response="$(mktemp)"
trap 'rm -f "$readiness_response" "$events_response"' EXIT

deadline=$((SECONDS + readiness_timeout_seconds))
ready=false
while (( SECONDS < deadline )); do
  if curl --fail --silent --show-error --output "$readiness_response" "$readiness_url" \
      && python3 - "$readiness_response" <<'PY'; then
import json
import sys

with open(sys.argv[1], encoding="utf-8") as response:
    assert json.load(response).get("status") == "UP"
PY
    ready=true
    break
  fi

  echo "Waiting for production readiness at $readiness_url..."
  sleep "$readiness_poll_seconds"
done

if [[ "$ready" != true ]]; then
  echo "Production image did not become ready within ${readiness_timeout_seconds} seconds: $readiness_url" >&2
  exit 1
fi

echo "Readiness is UP; verifying Liquibase applied changelog records."
changelog_count="$(compose exec --no-TTY postgres psql -U loopin_smoke -d loopin_smoke -tAc \
  'SELECT COUNT(*) FROM databasechangelog;')"
if ! [[ "$changelog_count" =~ ^[1-9][0-9]*$ ]]; then
  echo "Liquibase databasechangelog has no applied records (count: '${changelog_count}')." >&2
  exit 1
fi

echo "Liquibase applied $changelog_count changelog record(s); checking public events API."
curl --fail --silent --show-error --output "$events_response" "$events_url"
python3 - "$events_response" <<'PY'
import json
import sys

with open(sys.argv[1], encoding="utf-8") as response:
    json.load(response)
PY

echo "Production-image smoke test passed."
