#!/usr/bin/env bash
# Warms the Gradle dependency cache for the whole build by running the
# prefetchDependencies task in every module: downloads every external
# compile/runtime dependency (and runs their artifact transforms) without
# compiling anything. Safe to run at any time; a no-op once the cache is warm.
#
# .githooks/post-checkout runs this in the background after branch switches when
# prefetch.onCheckout=true is set in gradle.properties (repo or ~/.gradle), so a
# freshly checked-out branch never makes IDE sync wait on downloads.
set -o errexit
set -o pipefail
set -o nounset

git_root=$(git rev-parse --show-toplevel)
cd "$git_root"

# One prefetch at a time per checkout: rapid branch hops must not stack up Gradle
# invocations. mkdir is atomic and portable (macOS ships no flock).
lock_dir="$git_root/.gradle/prefetch-dependencies.lock"
mkdir -p "$git_root/.gradle"
if ! mkdir "$lock_dir" 2>/dev/null; then
  echo "prefetch already running (lock: $lock_dir)" >&2
  exit 0
fi
trap 'rmdir "$lock_dir" 2>/dev/null || true' EXIT

# --quiet keeps the per-module "Pre-fetched N artifact(s)" lines; pass -q or
# extra Gradle flags through "$@" to tune.
./gradlew prefetchDependencies --quiet "$@"
