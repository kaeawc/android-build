#!/usr/bin/env bash
# Installs the pinned Krit release (https://github.com/kaeawc/krit) into $KRIT_INSTALL_DIR and
# prints the binary's path. The tarball is verified against the checksum pinned here, so a
# release asset that changes after review fails the install.
#
# CI installs Krit with the kaeawc/krit action instead (.github/workflows/commit.yml); this script
# is for local runs. Keep KRIT_VERSION in step with that action and with the
# dev.jasonpearson.krit.settings plugin in settings.gradle.kts. To update: bump all three, replace
# the checksums below with the release's checksums.txt entries, and regenerate the baseline
# (ci/validate_krit.sh --update-baseline): its entries include each finding's message, which can
# change between releases.

set -euo pipefail

KRIT_VERSION="0.5.0"
KRIT_INSTALL_DIR=${KRIT_INSTALL_DIR:-"$HOME/.cache/krit/$KRIT_VERSION"}

libc=""
if [[ "$(uname -s)" == "Linux" ]] && ldd --version 2>&1 | grep -qi musl; then
  libc="-musl"
fi

case "$(uname -s)$libc-$(uname -m)" in
  Linux-musl-x86_64) platform="linux_musl_amd64"; sha256="5cd97737b642ace7742ff4cd05ad3d15d5e22471fc9e4ec0e81ccfadba3c7955" ;;
  Linux-x86_64) platform="linux_amd64"; sha256="e75a2a75cac8b40313f2b2b997ab769da7e1b324ae3ec38386854fdbd9588d42" ;;
  Linux-aarch64 | Linux-arm64) platform="linux_arm64"; sha256="a28c8f7ecd6e03f28f2091dc5c31518210a8349229807ca53e3a8d46abf914c6" ;;
  Darwin-arm64) platform="darwin_arm64"; sha256="9345eaac79ed50c50873f7b1fe14dd1d667259226a01a06ff7ee3d6dddc134f1" ;;
  Darwin-x86_64) platform="darwin_amd64"; sha256="d7785157c7d80da803e4a53e4ea674a04795f24c6bbed7e40598ff36eaa746c9" ;;
  *)
    echo "No Krit $KRIT_VERSION release for $(uname -s) $(uname -m)" >&2
    exit 1
    ;;
esac

if [[ -x "$KRIT_INSTALL_DIR/krit" ]]; then
  echo "$KRIT_INSTALL_DIR/krit"
  exit 0
fi

archive="krit_${KRIT_VERSION}_${platform}.tar.gz"
tmp_dir=$(mktemp -d)
trap 'rm -rf "$tmp_dir"' EXIT

echo "Installing Krit $KRIT_VERSION ($platform)..." >&2
curl -fsSL --retry 3 -o "$tmp_dir/$archive" \
  "https://github.com/kaeawc/krit/releases/download/v$KRIT_VERSION/$archive"

if command -v sha256sum &>/dev/null; then
  actual=$(sha256sum "$tmp_dir/$archive" | cut -d' ' -f1)
else
  actual=$(shasum -a 256 "$tmp_dir/$archive" | cut -d' ' -f1)
fi
if [[ "$actual" != "$sha256" ]]; then
  echo "Checksum mismatch for $archive: expected $sha256, got $actual" >&2
  exit 1
fi

tar -xzf "$tmp_dir/$archive" -C "$tmp_dir" krit
mkdir -p "$KRIT_INSTALL_DIR"
mv "$tmp_dir/krit" "$KRIT_INSTALL_DIR/krit"
chmod +x "$KRIT_INSTALL_DIR/krit"
echo "$KRIT_INSTALL_DIR/krit"
