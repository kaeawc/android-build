#!/usr/bin/env bash
# Installs the pinned Krit release (https://github.com/kaeawc/krit) into $KRIT_INSTALL_DIR and
# prints the binary's path. The tarball is verified against the checksum pinned here, so a
# release asset that changes after review fails the install.
#
# To update: bump KRIT_VERSION and replace the checksums with the release's checksums.txt entries.
# An official setup action (kaeawc/krit#1058) would replace this script and let Dependabot bump
# the version. After a bump, regenerate the baseline (ci/validate_krit.sh --update-baseline): its
# entries include each finding's message, which can change between releases.

set -euo pipefail

KRIT_VERSION="0.3.0"
KRIT_INSTALL_DIR=${KRIT_INSTALL_DIR:-"$HOME/.cache/krit/$KRIT_VERSION"}

libc=""
if [[ "$(uname -s)" == "Linux" ]] && ldd --version 2>&1 | grep -qi musl; then
  libc="-musl"
fi

case "$(uname -s)$libc-$(uname -m)" in
  Linux-musl-x86_64) platform="linux_musl_amd64"; sha256="d99abf5ce2d45300d1c1b294d86069116eb17e2249d5afe04478909609195711" ;;
  Linux-x86_64) platform="linux_amd64"; sha256="0bde297c7a32b125e9cd7f88381dbb60e068ed52501199639a9a4c926b5f4d43" ;;
  Linux-aarch64 | Linux-arm64) platform="linux_arm64"; sha256="d8421a9172f6cf6785d2524695da37a15070154123a1b3cfc35b99aa50adcc90" ;;
  Darwin-arm64) platform="darwin_arm64"; sha256="b29218c662bae30ecd24bda9aef86fdcd388c64fbb02e9cebcb649b72e6ea600" ;;
  Darwin-x86_64) platform="darwin_amd64"; sha256="d230704b6f91bced637f30a639ad3a6a07ba28c6cb480000cce860cd3f1abf74" ;;
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
