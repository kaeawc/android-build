#!/usr/bin/env python3
"""Renames this template's placeholders for a new project.

    scripts/rename_template.py --package com.example.myapp --app-name "My App" --project-name my-app

  --package       the application id and root Kotlin package (replaces dev.jasonpearson.android),
                  including source directories, namespaces, manifests and BUILD files
  --app-name      the launcher label (app_name in app/src/main/res/values/strings.xml)
  --project-name  the Gradle root project and Bazel module name
  --dry-run       print what would change without writing anything

Only git-tracked files are touched, so run it on a clean checkout and review `git diff` after.
"""

import argparse
import re
import subprocess
import sys
from pathlib import Path

OLD_PACKAGE = "dev.jasonpearson.android"
OLD_APP_NAME = "Android"
OLD_PROJECT_NAME = "android-build"
OLD_MODULE_NAME = "android_build"
PACKAGE_RE = re.compile(r"^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+$")
SOURCE_ROOTS = ("kotlin", "java")


def tracked_files():
    output = subprocess.run(
        ["git", "ls-files", "-z"], check=True, capture_output=True, text=True
    ).stdout
    return [Path(p) for p in output.split("\0") if p]


def read_text(path):
    try:
        return path.read_text(encoding="utf-8")
    except (UnicodeDecodeError, IsADirectoryError, FileNotFoundError):
        return None


def replace_package(text, new_package):
    # Match the package as a whole identifier path: dev.jasonpearson.android and
    # dev.jasonpearson.android.feature..., but not dev.jasonpearson.androidx.
    dotted = re.compile(re.escape(OLD_PACKAGE) + r"(?![A-Za-z0-9_])")
    slashed = re.compile(re.escape(OLD_PACKAGE.replace(".", "/")) + r"(?![A-Za-z0-9_])")
    text = dotted.sub(new_package, text)
    return slashed.sub(new_package.replace(".", "/"), text)


def move_source_dirs(files, new_package, dry_run):
    old_parts = OLD_PACKAGE.split(".")
    new_parts = new_package.split(".")
    moved = set()
    for path in files:
        parts = path.parts
        for i, part in enumerate(parts):
            if part in SOURCE_ROOTS and tuple(parts[i + 1 : i + 1 + len(old_parts)]) == tuple(old_parts):
                old_dir = Path(*parts[: i + 1 + len(old_parts)])
                if old_dir in moved:
                    break
                moved.add(old_dir)
                new_dir = Path(*parts[: i + 1], *new_parts)
                print(f"move {old_dir} -> {new_dir}")
                if not dry_run:
                    new_dir.parent.mkdir(parents=True, exist_ok=True)
                    subprocess.run(["git", "mv", str(old_dir), str(new_dir)], check=True)
                    prune_empty_parents(old_dir.parent, Path(*parts[: i + 1]))
                break


def prune_empty_parents(directory, stop):
    while directory != stop and directory.exists() and not any(directory.iterdir()):
        directory.rmdir()
        directory = directory.parent


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--package", required=True)
    parser.add_argument("--app-name")
    parser.add_argument("--project-name")
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()

    if not PACKAGE_RE.match(args.package):
        sys.exit(f"--package must look like com.example.myapp, got {args.package!r}")
    if not Path(".git").exists():
        sys.exit("Run from the repository root.")

    files = tracked_files()
    for path in files:
        if path == Path(__file__).resolve().relative_to(Path.cwd().resolve()):
            continue  # Keep this script's own constants.
        text = read_text(path)
        if text is None:
            continue
        new = replace_package(text, args.package)
        if path == Path("app/src/main/res/values/strings.xml") and args.app_name:
            new = new.replace(
                f'<string name="app_name">{OLD_APP_NAME}</string>',
                f'<string name="app_name">{args.app_name}</string>',
            )
        if args.project_name:
            if path == Path("settings.gradle.kts"):
                new = new.replace(f'rootProject.name = "{OLD_PROJECT_NAME}"', f'rootProject.name = "{args.project_name}"')
            if path == Path("MODULE.bazel"):
                module = re.sub(r"[^a-z0-9_]", "_", args.project_name.lower())
                new = new.replace(f'module(name = "{OLD_MODULE_NAME}")', f'module(name = "{module}")')
        if new != text:
            print(f"edit {path}")
            if not args.dry_run:
                path.write_text(new, encoding="utf-8")

    move_source_dirs(files, args.package, args.dry_run)
    if not args.dry_run:
        print("\nDone. Review with `git status` / `git diff`, then build: bazel build //app && bazel test //...")


if __name__ == "__main__":
    main()
