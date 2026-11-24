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

KRIT_VERSION="0.4.0"
KRIT_INSTALL_DIR=${KRIT_INSTALL_DIR:-"$HOME/.cache/krit/$KRIT_VERSION"}

libc=""
if [[ "$(uname -s)" == "Linux" ]] && ldd --version 2>&1 | grep -qi musl; then
  libc="-musl"
fi

case "$(uname -s)$libc-$(uname -m)" in
  Linux-musl-x86_64) platform="linux_musl_amd64"; sha256="8a3d61da217d77297b13ca61d248918ad53103bcecdfe5647d876633975220ab" ;;
  Linux-x86_64) platform="linux_amd64"; sha256="4720196534b41a41a211103382289eeff1cabbd65522d6055fb81fe9d42944a7" ;;
  Linux-aarch64 | Linux-arm64) platform="linux_arm64"; sha256="843849a0f1aca85279b40cc6020eb7d9fc1302f2585e5da5050b597edffa59b1" ;;
  Darwin-arm64) platform="darwin_arm64"; sha256="0f34caa35c36a2d188e65d7138bf6fedae272dc414b825835ae924744d8b8cef" ;;
  Darwin-x86_64) platform="darwin_amd64"; sha256="40407614695e9c01176cb4f7006a583d824c489ebe1b8a7eeef68c75a8aed44e" ;;
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
