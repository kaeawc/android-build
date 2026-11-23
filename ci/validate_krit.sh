#!/usr/bin/env bash
# Runs Krit (krit.yml) over the repository and fails on any finding not in ci/krit-baseline.xml.
#
#   ci/validate_krit.sh                   check (installs the pinned Krit unless KRIT is set)
#   KRIT_SARIF=out.sarif ci/validate_krit.sh   also write the findings as SARIF (CI uploads it)
#   ci/validate_krit.sh --update-baseline      rewrite the baseline from the current findings
#
# Krit lists files with git, so ignored build outputs (build/, bazel-*) are skipped. Locally it
# keeps a daemon running (.krit/) that makes repeat runs near-instant; CI runs it in-process.

set -euo pipefail

cd "$(git rev-parse --show-toplevel)"

BASELINE="ci/krit-baseline.xml"
KRIT=${KRIT:-$(ci/install_krit.sh)}
KRIT_FLAGS=(--no-fir --base-path .)
if [[ -n "${CI:-}" ]]; then
  KRIT_FLAGS+=(--no-daemon)
fi

if [[ "${1:-}" == "--update-baseline" ]]; then
  "$KRIT" "${KRIT_FLAGS[@]}" --create-baseline "$BASELINE" -q . || true
  echo "Wrote $BASELINE"
  exit 0
fi

start_time=$(ci/get_timestamp.sh)

if [[ -n "${KRIT_SARIF:-}" ]]; then
  mkdir -p "$(dirname "$KRIT_SARIF")"
  "$KRIT" "${KRIT_FLAGS[@]}" --baseline "$BASELINE" -f sarif -o "$KRIT_SARIF" . && status=0 || status=$?
  # The second run, for readable findings in the log, reuses the first one's .krit/ caches.
  if [[ $status -ne 0 ]]; then
    "$KRIT" "${KRIT_FLAGS[@]}" --baseline "$BASELINE" -f plain . || true
  fi
else
  "$KRIT" "${KRIT_FLAGS[@]}" --baseline "$BASELINE" -f plain . && status=0 || status=$?
fi

end_time=$(ci/get_timestamp.sh)
echo "Total time elapsed: $((end_time - start_time)) ms."

if [[ $status -ne 0 ]]; then
  echo "Krit found new issues. Fix them, suppress one with @Suppress(\"RuleName\"), or, for an"
  echo "intentional exception, adjust krit.yml. Run ci/validate_krit.sh locally to reproduce."
  exit "$status"
fi

echo "No new Krit findings."
