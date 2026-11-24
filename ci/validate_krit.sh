#!/usr/bin/env bash
# Runs Krit (krit.yml) over the repository and fails on any finding not in ci/krit-baseline.xml.
#
#   ci/validate_krit.sh                        source-only check in a few seconds (pre-push hook)
#   KRIT_FIR=1 ci/validate_krit.sh             also run Krit's FIR checks, as CI does
#   KRIT_SARIF=out.sarif ci/validate_krit.sh   also write the findings as SARIF (CI uploads it)
#   ci/validate_krit.sh --update-baseline      rewrite the baseline from the current findings
#
# The FIR checks compile the sources with the Kotlin compiler, against each module's classpath and
# compiler plugins, which drops findings a source-only pass gets wrong and adds some it can't see.
# They need Java 21 or newer and the Gradle model: run `./gradlew kritExportModel` first, and again
# after a dependency or module change. A FIR pass that can't run fails the check rather than
# falling back to the source-only result.
#
# Krit installs the pinned version unless KRIT names a binary. It lists files with git, so ignored
# build outputs (build/, bazel-*) are skipped. Locally it keeps a daemon running (.krit/) that
# makes repeat runs near-instant; CI runs it in-process.

set -euo pipefail

cd "$(git rev-parse --show-toplevel)"

BASELINE="ci/krit-baseline.xml"
MODEL=".krit/gradle-model"
KRIT=${KRIT:-$(ci/install_krit.sh)}

common_flags=(--base-path .)
if [[ -n "${CI:-}" ]]; then
  common_flags+=(--no-daemon --no-fir-daemon)
fi
source_only_flags=("${common_flags[@]}" --no-fir)
fir_flags=("${common_flags[@]}" --fail-on-degraded)

require_model() {
  if [[ ! -d "$MODEL" ]]; then
    echo "Krit's FIR checks need the Gradle model ($MODEL). Run ./gradlew kritExportModel first." >&2
    exit 2
  fi
}

# The baseline lists the findings of both passes, so the source-only check (no Gradle needed)
# and CI's FIR check both pass on findings that predate them. It also lists each finding under
# both of the file names Krit 0.4.0 writes: run in-process (CI), it names Kotlin files by base
# name and manifests by repository path, and through its daemon (local runs) the other way round.
if [[ "${1:-}" == "--update-baseline" ]]; then
  require_model
  tmp_dir=$(mktemp -d)
  trap 'rm -rf "$tmp_dir"' EXIT
  in_process=(--base-path . --no-daemon --no-fir-daemon)
  "$KRIT" "${in_process[@]}" --no-fir --create-baseline "$tmp_dir/source.xml" -q . || true
  "$KRIT" "${in_process[@]}" --fail-on-degraded --create-baseline "$tmp_dir/fir.xml" -q . || true
  "$KRIT" --base-path . --no-fir --create-baseline "$tmp_dir/daemon.xml" -q . || true
  {
    echo '<?xml version="1.0" encoding="UTF-8"?>'
    echo '<SmellBaseline>'
    echo '  <ManuallySuppressedIssues></ManuallySuppressedIssues>'
    echo '  <CurrentIssues>'
    grep -h '^ *<ID>' "$tmp_dir"/*.xml | LC_ALL=C sort -u
    echo '  </CurrentIssues>'
    echo '</SmellBaseline>'
  } >"$BASELINE"
  echo "Wrote $BASELINE ($(grep -c '<ID>' "$BASELINE") findings)"
  exit 0
fi

if [[ "${KRIT_FIR:-}" == "1" ]]; then
  require_model
  KRIT_FLAGS=("${fir_flags[@]}")
else
  KRIT_FLAGS=("${source_only_flags[@]}")
fi

start_time=$(ci/get_timestamp.sh)

if [[ -n "${KRIT_SARIF:-}" ]]; then
  mkdir -p "$(dirname "$KRIT_SARIF")"
  "$KRIT" "${KRIT_FLAGS[@]}" --baseline "$BASELINE" -f sarif -o "$KRIT_SARIF" . && status=0 || status=$?
  # The second run, for readable findings in the log, reuses the first one's .krit/ caches.
  if [[ $status -eq 1 ]]; then
    "$KRIT" "${KRIT_FLAGS[@]}" --baseline "$BASELINE" -f plain . || true
  fi
else
  "$KRIT" "${KRIT_FLAGS[@]}" --baseline "$BASELINE" -f plain . && status=0 || status=$?
fi

end_time=$(ci/get_timestamp.sh)
echo "Total time elapsed: $((end_time - start_time)) ms."

if [[ $status -eq 1 ]]; then
  echo "Krit found new issues. Fix them, suppress one with @Suppress(\"RuleName\"), or, for an"
  echo "intentional exception, adjust krit.yml. Run ci/validate_krit.sh locally to reproduce."
  exit 1
elif [[ $status -ne 0 ]]; then
  echo "Krit failed (exit $status) before it could report findings; see the messages above."
  exit "$status"
fi

echo "No new Krit findings."
