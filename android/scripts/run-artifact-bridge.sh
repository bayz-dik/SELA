#!/usr/bin/env bash
# Supervisor untuk artifact-bridge.py (Maven bridge loopback 127.0.0.1:8766).
# Pelajaran 2026-10-14: proses background telanjang mati saat VM restart;
# pola supervisor loop-restart seperti run-codex-remote.sh.
set -uo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
while true; do
  echo "[bridge] starting artifact-bridge.py $(date -u +%FT%TZ)"
  /usr/bin/python3 "$SCRIPT_DIR/artifact-bridge.py"
  echo "[bridge] exited ($?), restart in 10s"
  sleep 10
done
