#!/usr/bin/env python3
"""Repins @maven, adding an ANDROIDX_ALIGNMENT entry for every strict-range version conflict.

AndroidX POMs pin sibling artifacts to exact versions ([1.7.0,1.7.0]). Gradle reconciles those
with dependency constraints; rules_jvm_external's Maven resolver fails on the first conflict
instead. This script runs `bazel run @maven//:pin`, pins the conflicting artifact to the highest
version anything asks for, and repeats until the pin succeeds.

Usage (from the repository root): bazel/scripts/align_androidx.py
"""

import json
import os
import re
import subprocess
import sys
from pathlib import Path

MODULE = Path("MODULE.bazel")
LIST_RE = re.compile(r"(ANDROIDX_ALIGNMENT = \[\n)(.*?)(\])", re.S)
CONFLICT_RE = re.compile(r"Could not resolve version conflict among \[(.*)\]")


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
    text = LIST_RE.sub(lambda m: m.group(1) + body + m.group(3), MODULE.read_text())
    MODULE.write_text(text)


def stale_roots():
    artifacts = json.loads(Path("maven_install.json").read_text())["artifacts"]
    versions = {}
    for key, value in artifacts.items():
        group, artifact = key.split(":")[:2]
        versions[f"{group}:{artifact}"] = value["version"]
    return {
        coords: versions[coords + "-android"]
        for coords, version in versions.items()
        if coords.startswith("androidx.")
        and coords + "-android" in versions
        and version_key(version) < version_key(versions[coords + "-android"])
    }


def main():
    pins = read_pins()
    for attempt in range(1, 60):
        result = subprocess.run(
            ["bazel", "run", "@maven//:pin"],
            env={**os.environ, "REPIN": "1"},
            capture_output=True,
            text=True,
        )
        output = result.stdout + result.stderr
        match = CONFLICT_RE.search(output)
        if not match:
            if result.returncode != 0:
                sys.stderr.write(output[-4000:])
                return result.returncode
            # An AndroidX root that resolved older than its own -android artifact is a pre-KMP
            # release with real classes: align it, or the APK gets two copies of each class.
            stale = stale_roots()
            if stale:
                for coords, version in stale.items():
                    print(f"Aligning {coords} on {version} (its -android artifact's version)")
                    pins[coords] = version
                write_pins(pins)
                continue
            print(f"Pinned after {attempt} attempt(s); {len(pins)} alignment pin(s).")
            return 0
        targets, parents = {}, {}
        for path in match.group(1).split(", "):
            nodes = [node.split(":") for node in path.split(" -> ")]
            group, artifact, _type, version = nodes[-1]
            targets.setdefault(f"{group}:{artifact}", set()).update(
                re.findall(r"[0-9][^,\[\]()]*", version)
            )
            if len(nodes) > 1:
                parent_group, parent_artifact, _type, parent_version = nodes[-2]
                parents.setdefault(f"{parent_group}:{parent_artifact}", set()).update(
                    re.findall(r"[0-9][^,\[\]()]*", parent_version)
                )
        # Pin the conflicting artifact to the highest version anything asks for. The artifacts
        # that declare the strict ranges need pinning too (the resolver still applies the ranges
        # of a parent that lost to a newer version): AndroidX releases a group's artifacts
        # together, so a parent in the same group takes the same version.
        chosen = {coords: max(c, key=version_key) for coords, c in targets.items()}
        for coords, candidates in parents.items():
            same_group = [v for t, v in chosen.items() if t.split(":")[0] == coords.split(":")[0]]
            chosen.setdefault(coords, same_group[0] if same_group else max(candidates, key=version_key))
        text = LIST_RE.sub("", MODULE.read_text())
        for coords, best in chosen.items():
            if f'"{coords}"' in text or f'"{coords}:' in text:
                continue  # Declared directly; its own version already wins.
            print(f"Aligning {coords} on {best}")
            pins[coords] = best
        write_pins(pins)
    print("Gave up after too many conflicts.", file=sys.stderr)
    return 1


if __name__ == "__main__":
    sys.exit(main())
