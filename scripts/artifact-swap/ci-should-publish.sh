#!/usr/bin/env bash
# Decide whether the Artifact Swap publish pipeline needs to run for a commit.
# The baseline is expected to be the last commit whose artifacts and BOM were published.
# Copyright (c) 2026 Jason Pearson

set -euo pipefail

if [[ $# -lt 1 || $# -gt 2 ]]; then
  printf 'usage: %s <head-sha> [<baseline-ref>]\n' "$0" >&2
  exit 2
fi

head_ref=$1
baseline_ref=${2:-refs/remotes/origin/artifact-swap-green-main}

if ! head_sha=$(git rev-parse --verify --quiet "${head_ref}^{commit}" 2>/dev/null); then
  printf 'ci-should-publish: head does not resolve: %s\n' "$head_ref" >&2
  exit 2
fi

if ! baseline_sha=$(git rev-parse --verify --quiet "${baseline_ref}^{commit}" 2>/dev/null); then
  printf 'publish: baseline branch not found\n'
  exit 0
fi

if git merge-base --is-ancestor "$baseline_sha" "$head_sha" >/dev/null 2>&1; then
  :
else
  merge_base_exit=$?
  if [[ $merge_base_exit -eq 1 ]]; then
    printf 'publish: baseline is not an ancestor of head\n'
    exit 0
  fi
  printf 'ci-should-publish: could not compare baseline and head\n' >&2
  exit 2
fi

if ! diff_output=$(git diff --name-only "$baseline_sha" "$head_sha"); then
  printf 'ci-should-publish: could not compute the baseline diff\n' >&2
  exit 2
fi

if [[ -z $diff_output ]]; then
  printf 'skip: no changed files\n'
  exit 1
fi

is_swap_relevant() {
  local path=$1

  case "$path" in
    docs/*|*.md|.githooks/*|patches/*|research/*|LICENSE*|.gitignore|.editorconfig)
      return 1
      ;;
    .github/*)
      case "$path" in
        .github/workflows/publish.yml|.github/actions/*)
          return 0
          ;;
        *)
          return 1
          ;;
      esac
      ;;
    scripts/*)
      case "$path" in
        scripts/artifact-swap/*)
          return 0
          ;;
        *)
          return 1
          ;;
      esac
      ;;
    *)
      return 0
      ;;
  esac
}

swap_count=0
first_swap_path=''
while IFS= read -r changed_path; do
  [[ -n $changed_path ]] || continue
  if is_swap_relevant "$changed_path"; then
    swap_count=$((swap_count + 1))
    if [[ -z $first_swap_path ]]; then
      first_swap_path=$changed_path
    fi
  fi
done <<< "$diff_output"

if [[ $swap_count -gt 0 ]]; then
  printf 'publish: %d swap-relevant file(s) changed (first: %s)\n' \
    "$swap_count" "$first_swap_path"
  exit 0
fi

changed_count=0
while IFS= read -r changed_path; do
  [[ -n $changed_path ]] || continue
  changed_count=$((changed_count + 1))
done <<< "$diff_output"
printf 'skip: %d changed file(s), none swap-relevant\n' "$changed_count"
exit 1
