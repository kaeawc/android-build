"""Hermetic JDK 27 (Eclipse Temurin) for Bazel.

rules_java ships remote JDKs only up to 25. This extension adds `remotejdk_27` the same way
rules_java defines its own: one `remote_java_repository` per platform, each registering a
runtime toolchain selected by `--java_runtime_version=remotejdk_27` (or `27`).

Bump: update _VERSION and the per-platform URLs/checksums from
https://api.adoptium.net/v3/assets/latest/27/hotspot?image_type=jdk&vendor=eclipse
"""

load("@rules_java//toolchains:remote_java_repository.bzl", "remote_java_repository")

_VERSION = "27"

_JDKS = {
    "remotejdk27_linux": struct(
        constraints = ["@platforms//os:linux", "@platforms//cpu:x86_64"],
        sha256 = "005e57ee860978acfeeff1be2b30f770664fe2b941e9b803c61c76992086d107",
        strip_prefix = "jdk-27.0.1.1+4",
        url = "https://github.com/adoptium/temurin27-binaries/releases/download/jdk-27.0.1.1%2B4/OpenJDK27U-jdk_x64_linux_hotspot_27.0.1.1_4.tar.gz",
    ),
    "remotejdk27_linux_aarch64": struct(
        constraints = ["@platforms//os:linux", "@platforms//cpu:aarch64"],
        sha256 = "5ddacf925244a00b60cac828b61ff2f67a68cd32b8dfe5cee1405e333b4589e5",
        strip_prefix = "jdk-27.0.1.0.2+1",
        url = "https://github.com/adoptium/temurin27-binaries/releases/download/jdk-27.0.1.0.2%2B1/OpenJDK27U-jdk_aarch64_linux_hotspot_27.0.1.0.2_1.tar.gz",
    ),
    "remotejdk27_macos_aarch64": struct(
        constraints = ["@platforms//os:macos", "@platforms//cpu:aarch64"],
        sha256 = "bd5ff11de5e247372e8b27df700938bd02fe8fe81e24cd297e1daeb027d3151b",
        strip_prefix = "jdk-27.0.1.1+4/Contents/Home",
        url = "https://github.com/adoptium/temurin27-binaries/releases/download/jdk-27.0.1.1%2B4/OpenJDK27U-jdk_aarch64_mac_hotspot_27.0.1.1_4.tar.gz",
    ),
    "remotejdk27_win": struct(
        constraints = ["@platforms//os:windows", "@platforms//cpu:x86_64"],
        sha256 = "5fac419f00d2256941a6b1253a827bb8b904da215a1ec0667035e5592811c1dc",
        strip_prefix = "jdk-27.0.1.0.2+1",
        url = "https://github.com/adoptium/temurin27-binaries/releases/download/jdk-27.0.1.0.2%2B1/OpenJDK27U-jdk_x64_windows_hotspot_27.0.1.0.2_1.zip",
    ),
}

JDK27_REPOS = list(_JDKS.keys())

def _jdk27_impl(module_ctx):
    for name, jdk in _JDKS.items():
        remote_java_repository(
            name = name,
            version = _VERSION,
            target_compatible_with = jdk.constraints,
            sha256 = jdk.sha256,
            strip_prefix = jdk.strip_prefix,
            urls = [jdk.url],
        )
    return module_ctx.extension_metadata(reproducible = True)

jdk27 = module_extension(implementation = _jdk27_impl)
