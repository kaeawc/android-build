# shellcheck shell=bash
# Shared environment for the Artifact Swap scripts. Source, don't execute.
#
# Single source of truth for the CLI version on the shell side. Keep in lockstep
# with `build-artifactswap` in gradle/libs.versions.toml and the
# xyz.block.artifactswap.settings plugin version in settings.gradle.kts (Gradle
# cannot read this file, so those two are declared separately).
ARTIFACTSWAP_VERSION="0.1.12"

# Absolute path to the CLI binary for a given repo root.
artifactswap_bin() {
  echo "$1/tools/artifactswap/artifactswap-$ARTIFACTSWAP_VERSION/bin/artifactswap"
}

# Install the CLI if the expected version is not present.
ensure_artifactswap_cli() {
  local git_root="$1"
  if [[ ! -x "$(artifactswap_bin "$git_root")" ]]; then
    "$git_root/scripts/artifact-swap/install-cli.sh"
  fi
}
