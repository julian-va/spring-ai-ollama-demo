#!/usr/bin/env bash
set -e

URL="${1:-http://keycloak:8080/realms/ollama-realm/.well-known/openid-configuration}"
MAX_WAIT="${2:-180}"

END=$(( $(date +%s) + MAX_WAIT ))

while true; do
  if curl -fsS --max-time 5 "${URL}" >/dev/null 2>&1; then
    break
  fi
  if [ "$(date +%s)" -gt "${END}" ]; then
    echo "Timed out waiting for ${URL}" >&2
    exit 1
  fi
  sleep 2
done

# If there are extra args, exec them (e.g., the java command)
if [ "$#" -gt 2 ]; then
  shift 2
  exec "$@"
fi

exit 0
