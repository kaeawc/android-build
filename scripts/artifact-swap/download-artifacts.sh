#!/usr/bin/env bash
# Syncs the Artifact Swap BOM branch and downloads pre-compiled artifacts from
# GitHub Packages into the local Maven repository, so IDE sync can swap unchanged
# projects for artifacts (set artifactswap.enabled=true once this has run).
#
# Auth: GitHub Packages requires an authenticated read even for public packages.
# Export SECRETS_PATH to a directory containing github-token.txt with a PAT that
# has the read:packages scope.
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

# Refresh the BOM tracking branch so the plugin can find the newest usable BOM.
if ! git fetch origin "$BRANCH" --quiet 2>/dev/null; then
  echo "warning: could not fetch origin/$BRANCH (no BOM published yet?)" >&2
fi

echo "Syncing artifacts for Artifact Swap"
"$bin_path" download-artifacts --dir "$git_root" "$@"
