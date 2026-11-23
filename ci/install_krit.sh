#!/usr/bin/env bash
# Installs the pinned Krit release (https://github.com/kaeawc/krit) into $KRIT_INSTALL_DIR and
# prints the binary's path. The tarball is verified against the checksum pinned here, so a
# release asset that changes after review fails the install.
#
# To update: bump KRIT_VERSION and replace the checksums with the release's checksums.txt entries.

set -euo pipefail

KRIT_VERSION="0.2.0"
KRIT_INSTALL_DIR=${KRIT_INSTALL_DIR:-"$HOME/.cache/krit/$KRIT_VERSION"}

libc=""
if [[ "$(uname -s)" == "Linux" ]] && ldd --version 2>&1 | grep -qi musl; then
  libc="-musl"
fi

case "$(uname -s)$libc-$(uname -m)" in
  Linux-musl-x86_64) platform="linux_musl_amd64"; sha256="9f5a9abf83455438ebc00b841dc4a661d009efc2e4976adea79caf779a86d084" ;;
  Linux-x86_64) platform="linux_amd64"; sha256="ccb39239e547e4141db7f73ec434f09cc3a9b99ed23a3779f586c05d5680408d" ;;
  Linux-aarch64 | Linux-arm64) platform="linux_arm64"; sha256="3006918a2da8f4e1c6cfe032d2688f138748cc6d0a7497686dfa9a3ad58a1f12" ;;
  Darwin-arm64) platform="darwin_arm64"; sha256="00e2170d07aa0aa53fce0dd8a4cc9a8a4f93b0f7858b38fb2bd2d8158c1b8151" ;;
  Darwin-x86_64) platform="darwin_amd64"; sha256="8c80fdbdd8267fc800e082bd58ef16362e375c861496a1579e48d22b430fb096" ;;
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
