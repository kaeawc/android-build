#!/usr/bin/env bash
# Syncs the Artifact Swap BOM branch and downloads pre-compiled artifacts from
# GitHub Packages into the local Maven repository, so IDE sync can swap unchanged
# projects for artifacts (set artifactswap.enabled=true once this has run).
#
# Auth: GitHub Packages requires an authenticated read even for public packages.
# Preferred: the GitHub CLI. Grant its token the packages scope once with
#     gh auth refresh -s read:packages
# and this script mints the token file from `gh auth token` automatically.
# Alternative: export SECRETS_PATH to a directory containing github-token.txt
# with a PAT that has the read:packages scope.
set -o errexit
set -o pipefail
set -o nounset

VERSION="0.1.12"
BRANCH="artifact-swap-green-main"
git_root=$(git rev-parse --show-toplevel)
bin_path="$git_root/tools/artifactswap/artifactswap-$VERSION/bin/artifactswap"

if [[ ! -x "$bin_path" ]]; then
  "$git_root/scripts/artifact-swap/install-cli.sh"
fi

# Resolve the GitHub Packages token: explicit SECRETS_PATH wins; otherwise mint
# one from the gh CLI.
if [[ -z "${SECRETS_PATH:-}" || ! -f "${SECRETS_PATH:-}/github-token.txt" ]]; then
  if command -v gh >/dev/null 2>&1 && gh auth token >/dev/null 2>&1; then
    scopes=$(gh auth status 2>/dev/null | grep -i "Token scopes" || true)
    if [[ -n "$scopes" && "$scopes" != *"packages"* ]]; then
      echo "error: the gh CLI token lacks the read:packages scope." >&2
      echo "Run:  gh auth refresh -s read:packages   and retry." >&2
      exit 1
    fi
    token_dir="${XDG_CONFIG_HOME:-$HOME/.config}/android-build"
    mkdir -p "$token_dir"
    chmod 700 "$token_dir"
    gh auth token > "$token_dir/github-token.txt"
    chmod 600 "$token_dir/github-token.txt"
    export SECRETS_PATH="$token_dir"
    echo "Using GitHub Packages token from the gh CLI (SECRETS_PATH=$token_dir)"
  else
    echo "error: no GitHub Packages credentials found." >&2
    echo "Either install and authenticate the GitHub CLI (gh auth login," >&2
    echo "then gh auth refresh -s read:packages), or export SECRETS_PATH to a" >&2
    echo "directory containing github-token.txt with a read:packages PAT." >&2
    exit 1
  fi
fi

# Refresh the BOM tracking branch so the plugin can find the newest usable BOM.
if ! git fetch origin "$BRANCH" --quiet 2>/dev/null; then
  echo "warning: could not fetch origin/$BRANCH (no BOM published yet?)" >&2
fi

echo "Syncing artifacts for Artifact Swap"
"$bin_path" download-artifacts --dir "$git_root" "$@"
