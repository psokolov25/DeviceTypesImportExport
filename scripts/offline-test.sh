#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$REPO_ROOT"

MAVEN_LOCAL_REPO="${MAVEN_LOCAL_REPO:-.m2/repository}"
OFFLINE_ARGS=(-o "-Dmaven.repo.local=${MAVEN_LOCAL_REPO}")

if [[ -x "./mvnw" ]]; then
  echo "[offline-test] Running Maven Wrapper in offline mode..."
  ./mvnw "${OFFLINE_ARGS[@]}" -pl device-template-library,device-template-demo-service test
  exit 0
fi

if command -v mvn >/dev/null 2>&1; then
  echo "[offline-test] Maven Wrapper is unavailable, running system Maven in offline mode..."
  mvn "${OFFLINE_ARGS[@]}" -pl device-template-library,device-template-demo-service test
  exit 0
fi

echo "[offline-test] ERROR: Neither ./mvnw nor mvn is available."
exit 1
