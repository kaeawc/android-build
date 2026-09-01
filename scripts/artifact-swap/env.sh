# shellcheck shell=bash
# Shared environment for the Artifact Swap scripts. Source, don't execute.
#
# Single source of truth for the CLI version on the shell side. The CLI is a
# patched build of upstream v0.1.12 (see patches/artifact-swap/), hosted as a
# release asset on this repo; the Gradle plugins stay on stock 0.1.12
# (`build-artifactswap` in gradle/libs.versions.toml and the settings plugin
# version in settings.gradle.kts).
ARTIFACTSWAP_VERSION="0.1.12-kaeawc.1"

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
