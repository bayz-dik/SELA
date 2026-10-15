#!/usr/bin/env bash
# Local sandbox entry point. Official Gradle wrapper remains unmodified.
set -euo pipefail
cd "$(dirname "$0")/.."
source scripts/local-env.sh
bridge_pid=''
cleanup() { if [ -n "$bridge_pid" ]; then kill "$bridge_pid" 2>/dev/null || true; fi; }
trap cleanup EXIT
if ! curl --noproxy '*' --silent --fail --max-time 5 "$SELA_MAVEN_BRIDGE/central/junit/junit/4.13.2/junit-4.13.2.pom" >/dev/null; then
    python scripts/artifact-bridge.py >"$GRADLE_USER_HOME/artifact-bridge.log" 2>&1 &
    bridge_pid=$!
    for attempt in $(seq 1 30); do
        if curl --noproxy '*' --silent --fail --max-time 5 "$SELA_MAVEN_BRIDGE/central/junit/junit/4.13.2/junit-4.13.2.pom" >/dev/null; then break; fi
        sleep 1
    done
fi
bash scripts/local-gradle.sh "${@:-test}"
