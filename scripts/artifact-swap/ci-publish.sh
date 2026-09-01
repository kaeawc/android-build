#!/usr/bin/env bash
# CI pipeline for Artifact Swap publishing, run on green pushes to main:
#   1. hashing          - content-hash every module
#   2. task-finder      - enumerate the per-module publish tasks
#   3. artifact-checker - drop tasks whose artifact already exists in GitHub Packages
#   4. task-runner      - publish only the missing artifacts (content-hash versions)
#   5. bom-publisher    - publish the BOM mapping each module to its hash version
# The workflow then fast-forwards the artifact-swap-green-main branch to this commit
# so developer syncs can discover the BOM.
#
# Requires: SECRETS_PATH pointing at a directory containing github-token.txt with a
# token that has read:packages + write:packages (in Actions, the built-in
# GITHUB_TOKEN with the packages permissions).
set -o errexit
set -o pipefail
set -o nounset

script_dir=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
# shellcheck source=scripts/artifact-swap/env.sh
source "$script_dir/env.sh"

git_root=$(git rev-parse --show-toplevel)
# Anchor all relative output paths (artifactswapHashes/, taskOutputs/) to the repo
# root: the Gradle publish plugin resolves artifactswap.artifactVersionFile against
# rootDir, so running from a subdirectory would otherwise split the two locations.
cd "$git_root"

ensure_artifactswap_cli "$git_root"
bin_path=$(artifactswap_bin "$git_root")

export ARTIFACTSWAP_TOOL_OPTS="-Xmx2048M"
hash_file="artifactswapHashes/hashing.out"
mkdir -p artifactswapHashes taskOutputs

echo "===== Hashing modules ====="
"$bin_path" hashing \
  --dir "$git_root" \
  --logging INFO \
  --hashing-output-file "$hash_file" \
  --log-gradle

echo "===== Finding publish tasks ====="
# Note: no -Partifactswap.publishingEnabled=true here. This repo applies the Artifact
# Swap publish plugin through the androidbuild.publish convention (gated on the
# version-file property) instead of the settings-plugin auto-apply.
"$bin_path" task-finder \
  --dir "$git_root" \
  --logging INFO \
  --gradle-args "-Partifactswap.artifactVersionFile=$hash_file" \
  --task-list-output-directory "taskOutputs" \
  --task publishToArtifactSwapRepository \
  --log-gradle \
  --output-mode "SINGLE_TASK_LIST"

echo "===== Checking existing artifacts in GitHub Packages ====="
"$bin_path" artifact-checker \
  --dir "$git_root" \
  --logging INFO \
  --hash-file "$hash_file" \
  --input-file "taskOutputs/task-output.out" \
  --output-file "taskOutputs/task-output-filtered.out"

echo "===== Publishing missing artifacts ====="
"$bin_path" task-runner \
  --dir "$git_root" \
  --logging INFO \
  --gradle-args "-Partifactswap.artifactVersionFile=$hash_file" \
  --task-list-file "taskOutputs/task-output-filtered.out" \
  --log-gradle

echo "===== Publishing BOM ====="
"$bin_path" bom-publisher \
  --dir "$git_root" \
  --logging INFO \
  --hash-file-location "$hash_file" \
  --bom-version "$(git rev-parse HEAD)"

echo "Artifact Swap publish pipeline complete"
