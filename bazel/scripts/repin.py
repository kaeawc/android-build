#!/usr/bin/env python3
"""Repins @maven so Bazel resolves the same dependency versions as Gradle.

rules_jvm_external's Maven resolver differs from Gradle in two ways that break an Android app:

- Version conflicts resolve to the *nearest* version, not the highest. A library that needs a
  newer androidx.collection than the one closest to the root gets the old one, and the app
  crashes at runtime with ClassNotFoundException.
- AndroidX POMs pin sibling artifacts to exact versions ([1.7.0,1.7.0]), which Gradle reconciles
  with dependency constraints and the Maven resolver rejects.

So this script treats Gradle's resolution of the app as the source of truth. It repins, and pins
(in GRADLE_ALIGNMENT in MODULE.bazel) every artifact that resolved older than Gradle resolves
it, every artifact in a strict-range conflict, AndroidX roots that resolved older than their
-android artifact, and -android/-jvm variants Gradle uses that Bazel never fetched, until nothing
changes. It also fails if an artifact declared in MODULE.bazel is older than Gradle's version,
which means MODULE.bazel and gradle/libs.versions.toml drifted.

Usage (from the repository root):
    bazel/scripts/repin.py           repin and update GRADLE_ALIGNMENT
    bazel/scripts/repin.py --check   only report what would change (exit 1 if anything would)
"""

import argparse
import json
import os
import re
import subprocess
import sys
from pathlib import Path

MODULE = Path("MODULE.bazel")
LOCK = Path("maven_install.json")
JVM_TEST_VARIANTS_BUILD = Path("third_party/jvm_test_variants/BUILD.bazel")
LIST_RE = re.compile(r"(GRADLE_ALIGNMENT = \[\n)(.*?)(\])", re.S)
VARIANTS_RE = re.compile(r"(GRADLE_VARIANTS = \{\n)(.*?)(\})", re.S)
JVM_TEST_RE = re.compile(r"(JVM_TEST_VARIANTS = \[\n)(.*?)(\])", re.S)
CONFLICT_RE = re.compile(r"Could not resolve version conflict among \[(.*)\]")
# A Gradle dependency report line: group:artifact:version, optionally "-> resolved".
GRADLE_DEP_RE = re.compile(r"([\w.\-]+):([\w.\-]+):(\{[^}]*\}|[\w.\-+]+)(?: -> ([\w.\-+]+))?")
GRADLE_CONFIGURATIONS = ["releaseRuntimeClasspath", "debugAndroidTestRuntimeClasspath"]
VARIANT_SUFFIXES = ("-android", "-jvm")


def version_key(version):
    main, _, pre = version.partition("-")
    numbers = tuple(int(n) for n in re.findall(r"\d+", main))
    return numbers, pre == "", pre


def read_pins():
    body = LIST_RE.search(MODULE.read_text()).group(2)
    return dict(
        line.strip().strip('",').rsplit(":", 1)
        for line in body.splitlines()
        if line.strip().startswith('"')
    )


def write_pins(pins):
    body = "".join(f'    "{coords}:{version}",\n' for coords, version in sorted(pins.items()))
    MODULE.write_text(LIST_RE.sub(lambda m: m.group(1) + body + m.group(3), MODULE.read_text()))


def read_variants():
    body = VARIANTS_RE.search(MODULE.read_text()).group(2)
    return dict(re.findall(r'"([^"]+)": "([^"]+)"', body))


def write_variants(variants):
    body = "".join(f'    "{root}": "{variant}",\n' for root, variant in sorted(variants.items()))
    MODULE.write_text(VARIANTS_RE.sub(lambda m: m.group(1) + body + m.group(3), MODULE.read_text()))


def missing_variants(gradle, variants):
    """coords -> replacement for GRADLE_VARIANTS, matching the variants Gradle picks for the app:

    - a multiplatform root Bazel has but that doesn't depend on the variant Gradle uses (a root
      POM doesn't always list its variants): redirect the root to the variant;
    - a -jvm variant in the lock that Gradle replaces with the -android one (e.g. a JVM-only POM
      depending on datastore-core-jvm): redirect it, or the APK gets both copies of every class.
    """
    lock = json.loads(LOCK.read_text())
    locked = locked_versions()
    depends_on = {}
    for key, deps in lock.get("dependencies", {}).items():
        depends_on[":".join(key.split(":")[:2])] = {":".join(d.split(":")[:2]) for d in deps}
    missing = {}
    for coords in gradle:
        root = next((coords[: -len(s)] for s in VARIANT_SUFFIXES if coords.endswith(s)), None)
        if root and root in locked and coords not in depends_on.get(root, set()):
            missing[root] = coords
    for coords in locked:
        if coords.endswith("-jvm") and coords not in gradle:
            android = coords[: -len("-jvm")] + "-android"
            if android in gradle:
                missing[coords] = android
    return {c: v for c, v in missing.items() if variants.get(c) != v}


def jvm_test_variants(variants):
    """The -jvm artifacts GRADLE_VARIANTS redirects away, which JVM unit tests still need."""
    locked = locked_versions()
    return sorted(f"{c}:{locked[c]}" for c in variants if c.endswith("-jvm") and c in locked)


def label(coords):
    return re.sub(r"[.:\-]", "_", ":".join(coords.split(":")[:2]))


def jvm_test_variants_build(entries):
    deps = "".join(f'        "@maven_jvm_test_variants//:{label(c)}",\n' for c in entries)
    return f"""load("@rules_java//java:defs.bzl", "java_library")

# Generated by bazel/scripts/repin.py: the -jvm variants of libraries the APK uses as -android
# (GRADLE_VARIANTS in MODULE.bazel). Every unit test puts this first on its runtime classpath, so
# tests on the plain JVM load these classes instead of the Android ones.
java_library(
    name = "jvm_test_variants",
    testonly = True,
    visibility = ["//visibility:public"],
    runtime_deps = [
{deps}    ],
)
"""


def sync_jvm_test_variants(variants, write):
    """Returns True when JVM_TEST_VARIANTS and its BUILD file already match GRADLE_VARIANTS."""
    entries = jvm_test_variants(variants)
    current = re.findall(r'"([^"]+)"', JVM_TEST_RE.search(MODULE.read_text()).group(2))
    build = jvm_test_variants_build(entries)
    in_sync = current == entries and JVM_TEST_VARIANTS_BUILD.exists() and JVM_TEST_VARIANTS_BUILD.read_text() == build
    if write and not in_sync:
        body = "".join(f'    "{c}",\n' for c in entries)
        MODULE.write_text(JVM_TEST_RE.sub(lambda m: m.group(1) + body + m.group(3), MODULE.read_text()))
        JVM_TEST_VARIANTS_BUILD.write_text(build)
        result = subprocess.run(
            ["bazel", "run", "@maven_jvm_test_variants//:pin"],
            env={**os.environ, "REPIN": "1"},
            capture_output=True,
            text=True,
        )
        if result.returncode != 0:
            sys.stderr.write((result.stdout + result.stderr)[-4000:])
            raise SystemExit(1)
    return in_sync


def declared_artifacts():
    """group:artifact -> version for the artifacts MODULE.bazel lists directly."""
    text = LIST_RE.sub("", MODULE.read_text())
    return {
        f"{group}:{artifact}": version
        for group, artifact, version in re.findall(r'"([\w.\-]+):([\w.\-]+):([\w.\-]+)"', text)
    }


def locked_versions():
    artifacts = json.loads(LOCK.read_text())["artifacts"]
    return {":".join(key.split(":")[:2]): value["version"] for key, value in artifacts.items()}


def gradle_versions():
    """group:artifact -> version Gradle resolves for the app (the highest, after constraints)."""
    resolved = {}
    for configuration in GRADLE_CONFIGURATIONS:
        report = subprocess.run(
            ["./gradlew", "-q", ":app:dependencies", "--configuration", configuration],
            check=True,
            capture_output=True,
            text=True,
        ).stdout
        for line in report.splitlines():
            if "project :" in line:
                continue
            match = GRADLE_DEP_RE.search(line)
            if not match:
                continue
            group, artifact, requested, result = match.groups()
            version = result or requested
            if version.startswith("{"):
                continue
            coords = f"{group}:{artifact}"
            if coords not in resolved or version_key(version) > version_key(resolved[coords]):
                resolved[coords] = version
    return resolved


def gradle_version_for(coords, gradle):
    if coords in gradle:
        return gradle[coords]
    for suffix in VARIANT_SUFFIXES:
        if coords.endswith(suffix) and coords[: -len(suffix)] in gradle:
            return gradle[coords[: -len(suffix)]]
    return None


def pin():
    """Runs the resolver; returns (succeeded, strict-range conflict paths or None, output)."""
    result = subprocess.run(
        ["bazel", "run", "@maven//:pin"],
        env={**os.environ, "REPIN": "1"},
        capture_output=True,
        text=True,
    )
    output = result.stdout + result.stderr
    match = CONFLICT_RE.search(output)
    return result.returncode == 0, (match.group(1).split(", ") if match else None), output


def conflict_pins(paths):
    """Pins for a strict-range conflict: the conflicting artifact at the highest version asked
    for, and the artifacts declaring the ranges (the resolver applies a losing parent's ranges
    too); a parent in the same AndroidX group takes the same version, as AndroidX releases a
    group's artifacts together."""
    targets, parents = {}, {}
    for path in paths:
        nodes = [node.split(":") for node in path.split(" -> ")]
        group, artifact, _type, version = nodes[-1]
        targets.setdefault(f"{group}:{artifact}", set()).update(re.findall(r"[0-9][^,\[\]()]*", version))
        if len(nodes) > 1:
            group, artifact, _type, version = nodes[-2]
            parents.setdefault(f"{group}:{artifact}", set()).update(re.findall(r"[0-9][^,\[\]()]*", version))
    chosen = {coords: max(candidates, key=version_key) for coords, candidates in targets.items()}
    for coords, candidates in parents.items():
        same_group = [v for t, v in chosen.items() if t.split(":")[0] == coords.split(":")[0]]
        chosen.setdefault(coords, same_group[0] if same_group else max(candidates, key=version_key))
    return chosen


def lagging_pins(gradle):
    """Pins for artifacts resolved older than Gradle resolves them, or older than their own
    -android artifact (a pre-KMP AndroidX release with real classes would duplicate them)."""
    locked = locked_versions()
    pins = {}
    for coords, version in locked.items():
        wanted = gradle_version_for(coords, gradle)
        android = locked.get(coords + "-android") if coords.startswith("androidx.") else None
        if android and (wanted is None or version_key(android) > version_key(wanted)):
            wanted = android
        if wanted and version_key(wanted) > version_key(version):
            pins[coords] = wanted
    return pins


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--check", action="store_true", help="report only; exit 1 if anything would change")
    args = parser.parse_args()

    gradle = gradle_versions()
    declared = declared_artifacts()
    drift = {
        coords: (version, gradle_version_for(coords, gradle))
        for coords, version in declared.items()
        if gradle_version_for(coords, gradle)
        and version_key(gradle_version_for(coords, gradle)) > version_key(version)
    }
    for coords, (version, wanted) in sorted(drift.items()):
        print(f"MODULE.bazel declares {coords}:{version}, Gradle resolves {wanted}: update MODULE.bazel")
    if drift:
        return 1

    pins = read_pins()
    variants = read_variants()
    original = dict(pins)
    for attempt in range(1, 60):
        if args.check:
            changes = {c: v for c, v in lagging_pins(gradle).items() if c not in declared}
            for coords, version in sorted(changes.items()):
                print(f"{coords} resolves older than Gradle's {version}")
            missing = missing_variants(gradle, read_variants())
            for root, variant in sorted(missing.items()):
                print(f"{root} should resolve to {variant}, as in Gradle")
            tests_in_sync = sync_jvm_test_variants(read_variants(), write=False)
            if not tests_in_sync:
                print("JVM_TEST_VARIANTS / third_party/jvm_test_variants don't match GRADLE_VARIANTS")
            return 1 if changes or missing or not tests_in_sync else 0
        succeeded, conflict, output = pin()
        missing = {}
        if conflict:
            new = conflict_pins(conflict)
        elif not succeeded:
            sys.stderr.write(output[-4000:])
            return 1
        else:
            new = lagging_pins(gradle)
            missing = missing_variants(gradle, variants)
            if missing:
                for root, variant in sorted(missing.items()):
                    print(f"Redirecting {root} to {variant}")
                    if variant not in locked_versions():
                        new[variant] = gradle[variant]
                variants.update(missing)
                write_variants(variants)
        new = {c: v for c, v in new.items() if c not in declared and pins.get(c) != v}
        if not new and not missing:
            sync_jvm_test_variants(variants, write=True)
            changed = len(set(pins.items()) ^ set(original.items()))
            print(f"Pinned after {attempt} attempt(s); {len(pins)} alignment pin(s), {changed} changed.")
            return 0
        for coords, version in sorted(new.items()):
            print(f"Aligning {coords} on {version}")
        pins.update(new)
        write_pins(pins)
    print("Gave up after too many attempts.", file=sys.stderr)
    return 1


if __name__ == "__main__":
    sys.exit(main())
