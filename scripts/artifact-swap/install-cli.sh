#!/usr/bin/env bash
# Downloads the Artifact Swap CLI (from Maven Central) into tools/artifactswap/.
# The CLI drives hashing, artifact download, publishing, and BOM management.
set -o errexit
set -o pipefail
set -o nounset

script_dir=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
# shellcheck source=scripts/artifact-swap/env.sh disable=SC1091
source "$script_dir/env.sh"

git_root=$(git rev-parse --show-toplevel)
tools_dir="$git_root/tools/artifactswap"
bin_path=$(artifactswap_bin "$git_root")

if [[ -x "$bin_path" ]]; then
  echo "Artifact Swap CLI $ARTIFACTSWAP_VERSION already installed at $bin_path"
  exit 0
fi

mkdir -p "$tools_dir"
tmp_zip=$(mktemp -t artifactswap-cli.XXXXXX)
trap 'rm -f "$tmp_zip"' EXIT

echo "Downloading Artifact Swap CLI $ARTIFACTSWAP_VERSION..."
curl -sL --fail --max-time 300 \
  "https://repo.maven.apache.org/maven2/xyz/block/artifactswap/cli/$ARTIFACTSWAP_VERSION/cli-$ARTIFACTSWAP_VERSION.zip" \
  -o "$tmp_zip"
unzip -q -o "$tmp_zip" -d "$tools_dir"
chmod +x "$bin_path"
echo "Installed: $bin_path"
