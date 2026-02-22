#!/usr/bin/env bash
set -euo pipefail

ACTIVE_PORT="${1:-}"

if [[ -z "${ACTIVE_PORT}" ]]; then
  echo "Usage: $0 <active-port>" >&2
  exit 1
fi

if [[ "${ACTIVE_PORT}" != "8080" && "${ACTIVE_PORT}" != "8081" ]]; then
  echo "active-port must be 8080 or 8081" >&2
  exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TARGETS_FILE="${TARGETS_FILE:-${SCRIPT_DIR}/../targets/backend-active.json}"
TARGETS_DIR="$(dirname "${TARGETS_FILE}")"
PROMETHEUS_RELOAD_URL="${PROMETHEUS_RELOAD_URL:-http://127.0.0.1:${PROMETHEUS_PORT:-9090}/-/reload}"

mkdir -p "${TARGETS_DIR}"

TMP_FILE="$(mktemp)"
trap 'rm -f "${TMP_FILE}"' EXIT

cat > "${TMP_FILE}" <<EOF
[
  {
    "targets": ["host.docker.internal:${ACTIVE_PORT}"],
    "labels": {
      "instance": "konect-backend",
      "service": "backend"
    }
  }
]
EOF

mv "${TMP_FILE}" "${TARGETS_FILE}"

if ! curl -fsS -X POST "${PROMETHEUS_RELOAD_URL}" >/dev/null; then
  echo "warning: failed to reload Prometheus (${PROMETHEUS_RELOAD_URL})" >&2
fi

echo "Updated backend metrics target to host.docker.internal:${ACTIVE_PORT}"
