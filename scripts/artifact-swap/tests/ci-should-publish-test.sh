#!/usr/bin/env bash
# Exercise the artifact-swap publish decision against throwaway Git repositories.
# Copyright (c) 2026 Jason Pearson

set -euo pipefail

SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
GUARD_SCRIPT="$SCRIPT_DIR/../ci-should-publish.sh"
TEST_ROOT=$(mktemp -d)
trap 'rm -rf "$TEST_ROOT"' EXIT

pass_count=0
fail_count=0

fail() {
  printf 'FAIL: %s\n' "$1"
  fail_count=$((fail_count + 1))
}

pass() {
  printf 'PASS: %s\n' "$1"
  pass_count=$((pass_count + 1))
}

run_case() {
  local description=$1
  local expected_exit=$2
  shift 2

  local output
  local actual_exit
  set +e
  output=$("$@" 2>&1)
  actual_exit=$?
  set -e

  if [[ $actual_exit -eq $expected_exit ]]; then
    pass "$description"
  else
    fail "$description (expected exit $expected_exit, got $actual_exit; output: $output)"
  fi
}

init_repo() {
  local repo=$1
  mkdir -p "$repo"
  git -C "$repo" init -q
  git -C "$repo" config user.name 'Artifact Swap Test'
  git -C "$repo" config user.email 'artifact-swap-test@example.com'
  printf 'base\n' > "$repo/base.txt"
  git -C "$repo" add base.txt
  git -C "$repo" commit -q -m 'base'
  git -C "$repo" branch artifact-swap-green-main
  git -C "$repo" checkout -q -b work
}

commit_change() {
  local repo=$1
  local path=$2
  local content=$3
  mkdir -p "$(dirname -- "$repo/$path")"
  printf '%s\n' "$content" > "$repo/$path"
  git -C "$repo" add "$path"
  git -C "$repo" commit -q -m "change $path"
}

commit_mixed_change() {
  local repo=$1
  mkdir -p "$repo/docs" "$repo/core/common/src/main/kotlin"
  printf 'mixed docs\n' > "$repo/docs/mixed.md"
  printf 'mixed Kotlin\n' > "$repo/core/common/src/main/kotlin/Mixed.kt"
  git -C "$repo" add docs/mixed.md core/common/src/main/kotlin/Mixed.kt
  git -C "$repo" commit -q -m 'change docs and Kotlin source'
}

reset_worktree() {
  git -C "$repo" checkout -q work
  git -C "$repo" reset -q --hard artifact-swap-green-main
  git -C "$repo" clean -q -fd
}

run_guard() {
  local repo=$1
  shift
  git -C "$repo" -c core.fsmonitor=false \
    -c safe.directory="$repo" \
    rev-parse --verify HEAD >/dev/null
  (
    cd -- "$repo"
    "$GUARD_SCRIPT" "$@"
  )
}

repo=$TEST_ROOT/repo
init_repo "$repo"
head=$(git -C "$repo" rev-parse HEAD)
run_case 'missing baseline publishes' 0 run_guard "$repo" "$head" missing-baseline

git -C "$repo" checkout -q -b rewritten-head
commit_change "$repo" rewritten.txt rewritten
git -C "$repo" checkout -q -b rewritten-baseline artifact-swap-green-main
commit_change "$repo" baseline.txt baseline
git -C "$repo" checkout -q rewritten-head
rewritten_head=$(git -C "$repo" rev-parse HEAD)
run_case 'baseline that is not an ancestor publishes' 0 run_guard "$repo" "$rewritten_head" rewritten-baseline

reset_worktree
commit_change "$repo" docs/guide.txt docs
docs_head=$(git -C "$repo" rev-parse HEAD)
run_case 'docs-only change skips' 1 run_guard "$repo" "$docs_head" artifact-swap-green-main

reset_worktree
commit_change "$repo" README.md readme
readme_head=$(git -C "$repo" rev-parse HEAD)
run_case 'README.md change skips' 1 run_guard "$repo" "$readme_head" artifact-swap-green-main

reset_worktree
commit_change "$repo" .github/workflows/commit.yml commit
commit_workflow_head=$(git -C "$repo" rev-parse HEAD)
run_case '.github/workflows/commit.yml change skips' 1 run_guard "$repo" "$commit_workflow_head" artifact-swap-green-main

reset_worktree
commit_change "$repo" .github/workflows/publish.yml publish
publish_workflow_head=$(git -C "$repo" rev-parse HEAD)
run_case '.github/workflows/publish.yml change publishes' 0 run_guard "$repo" "$publish_workflow_head" artifact-swap-green-main

reset_worktree
commit_change "$repo" .github/actions/x/action.yml action
action_head=$(git -C "$repo" rev-parse HEAD)
run_case '.github/actions/x/action.yml change publishes' 0 run_guard "$repo" "$action_head" artifact-swap-green-main

reset_worktree
commit_change "$repo" scripts/prefetch-dependencies.sh prefetch
prefetch_head=$(git -C "$repo" rev-parse HEAD)
run_case 'scripts/prefetch-dependencies.sh change skips' 1 run_guard "$repo" "$prefetch_head" artifact-swap-green-main

reset_worktree
commit_change "$repo" scripts/artifact-swap/env.sh env
env_head=$(git -C "$repo" rev-parse HEAD)
run_case 'scripts/artifact-swap/env.sh change publishes' 0 run_guard "$repo" "$env_head" artifact-swap-green-main

reset_worktree
commit_change "$repo" core/common/src/main/kotlin/Foo.kt kotlin
kotlin_head=$(git -C "$repo" rev-parse HEAD)
run_case 'module Kotlin source change publishes' 0 run_guard "$repo" "$kotlin_head" artifact-swap-green-main

reset_worktree
commit_change "$repo" gradle/libs.versions.toml versions
versions_head=$(git -C "$repo" rev-parse HEAD)
run_case 'Gradle version catalog change publishes' 0 run_guard "$repo" "$versions_head" artifact-swap-green-main

reset_worktree
commit_mixed_change "$repo"
mixed_head=$(git -C "$repo" rev-parse HEAD)
run_case 'mixed docs and Kotlin change publishes' 0 run_guard "$repo" "$mixed_head" artifact-swap-green-main

baseline=$(git -C "$repo" rev-parse artifact-swap-green-main)
run_case 'no diff skips' 1 run_guard "$repo" "$baseline" artifact-swap-green-main

if [[ $fail_count -ne 0 ]]; then
  printf '%d case(s) passed, %d case(s) failed\n' "$pass_count" "$fail_count" >&2
  exit 1
fi

printf '%d case(s) passed\n' "$pass_count"
