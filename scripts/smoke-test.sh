#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
UI_DIR="$ROOT_DIR/workbench-ui"

log() {
  printf '\n==> %s\n' "$1"
}

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    printf 'Missing required command: %s\n' "$1" >&2
    exit 1
  fi
}

log "Checking local toolchain"
require_command java
require_command mvn
require_command node
require_command npm
java -version
mvn -version | sed -n '1,2p'
node --version
npm --version

log "Running backend Maven tests"
(cd "$ROOT_DIR" && mvn test)

log "Installing frontend dependencies"
if [ -f "$UI_DIR/package-lock.json" ]; then
  (cd "$UI_DIR" && npm ci)
else
  (cd "$UI_DIR" && npm install)
fi

log "Building frontend"
(cd "$UI_DIR" && npm run build)

log "Checking optional API health endpoint"
if command -v curl >/dev/null 2>&1; then
  if curl --fail --silent --max-time 2 http://localhost:8080/api/health >/tmp/architecture-workbench-health.json; then
    printf 'API health: '
    cat /tmp/architecture-workbench-health.json
    printf '\n'
  else
    printf 'API health skipped: backend is not running on http://localhost:8080\n'
  fi
else
  printf 'API health skipped: curl is not installed\n'
fi

log "Smoke test complete"
