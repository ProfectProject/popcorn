#!/usr/bin/env bash
set -euo pipefail

ENV_FILE="${1:-./.env}"

if [ ! -f "$ENV_FILE" ]; then
  echo "Env file not found: $ENV_FILE" >&2
  exit 1
fi

set -a
source "$ENV_FILE"
set +a

export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-local}"

./gradlew bootRun