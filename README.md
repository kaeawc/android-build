[![Commit](https://github.com/kaeawc/android-build/actions/workflows/commit.yml/badge.svg)](https://github.com/kaeawc/android-build/actions/workflows/commit.yml)

# Android Build

A modern, multi-module Android app and template repository: Jetpack Compose on Kotlin 2.4,
built with **Bazel** (Kotlin incremental compilation, hermetic JDK 27) and opened in
**Android Studio** through a Gradle build kept for the IDE. It's also where I try out build and CI
techniques, so the repo carries notes on JVM tuning and CI along with the code.

The sample app is a companion app for [jasonpearson.dev](https://www.jasonpearson.dev): articles
from a Ghost blog, talks, projects from GitHub, photography, and home-screen widgets. Replace the
features with your own; the build, module structure and CI are the template.

## Start a new project from this template

1. Click **Use this template** on GitHub (or clone the repository) and check out your copy.
2. Rename the placeholders: the application id and root package `dev.jasonpearson.android`, the
   app name "Android", and the project name `android-build`:

   ```
   scripts/rename_template.py --package com.example.myapp --app-name "My App" --project-name my-app
   ```

   It rewrites packages, source directories, namespaces, manifests and BUILD files in place (add
   `--dry-run` to preview). Review with `git diff`, then commit.
3. Build and test: `bazel build //app && bazel test //...` (requirements below).
4. Open the directory in Android Studio. It syncs the Gradle build.
5. Make the app yours: replace the sample features under `feature/`, `data/` and `client/`, the
   Ghost settings in `app/BUILD.bazel` and `gradle.properties`, the launcher icon in
   `app/src/main/res`, and the debug signing key `app/debug.keystore` (a standard debug key).

## What's inside

| | |
|---|---|
| Language & UI | Kotlin 2.4.21 (language 2.4), Jetpack Compose (BOM 2026.11.01), Material 3, [Haze](https://github.com/chrisbanes/haze) blur |
| Architecture | Navigation 3, ViewModels, [Metro](https://github.com/ZacSweers/metro) compile-time DI, coroutines/Flow |
| Data | Retrofit 3 + kotlinx.serialization, OkHttp 5, DataStore, Coil 3, Glance widgets |
| Build | Bazel 9.3 (rules_android, rules_kotlin with incremental compilation, rules_jvm_external) for the CLI and CI; Gradle 9.9 / AGP 9.5 with convention plugins for Android Studio |
| JDK & SDK | Temurin JDK 27 for every tool (Java 25 bytecode); compileSdk 37.2, targetSdk 37, minSdk 27 |
| Quality | Unit tests on every layer, an instrumentation test APK, [Krit](https://github.com/kaeawc/krit) static analysis, ktfmt, buildifier, the module graph enforced by Bazel visibility |
| CI | GitHub Actions: Bazel build/test with cached disk and repository caches, APK diffing (Diffuse), UI tests on emulator.wtf, Dependabot |

## Project structure

```
app/            the application: Activity, navigation shell, DI graph, BuildConfig
feature/*       screens (Compose): articles, talks, projects, photography, saved, settings, about, widget
data/*          repositories: caching, pagination, offline fallback
client/*        API clients: Ghost, GitHub
subsystem/*     cross-cutting services: analytics, experimentation, storage
foundation/*    design system, design assets, navigation keys and deep links
core/*          models, DI scopes, networking, common utilities (pure Kotlin)
bazel/          Bazel macros, toolchains (Kotlin, JDK 27) and scripts; see docs/bazel.md
build-logic/    Gradle convention plugins (Android Studio build)
```

Each module has a `BUILD.bazel` (Bazel) and a `build.gradle.kts` (Android Studio) over the same
`src/main/kotlin`, `src/test/kotlin` and `src/main/res` sources. The allowed dependencies between
layers are enforced by Bazel visibility (`bazel/defs.bzl`), so a dependency the architecture
doesn't allow fails the build:

```
app        -> anything
feature    -> data, client, subsystem, foundation, core
data       -> client, subsystem, core
client     -> core
subsystem  -> foundation, core
foundation -> foundation, core
core       -> core
```

## Building and testing

**Requirements:** [Bazelisk](https://github.com/bazelbuild/bazelisk) installed as `bazel`, and an
Android SDK in `$ANDROID_HOME` with `platforms;android-37.2` and `build-tools;37.0.0`. Bazel
downloads its own JDK 27. Android Studio (Gradle) also uses JDK 27 and downloads it if missing.

| What | Command |
|---|---|
| Debug APK | `bazel build //app` → `bazel-bin/app/app.apk` |
| Release APK (R8, resource shrinking) | `bazel build -c opt //app` |
| Install | `adb install bazel-bin/app/app.apk` |
| All unit tests | `bazel test //...` |
| One module's tests | `bazel test //data/articles:tests` |
| Instrumentation test APK | `bazel build //app:instrumentation_tests_apk` |
| Format BUILD files | `bazel run //bazel:buildifier` |
| Static analysis ([Krit](https://github.com/kaeawc/krit), `krit.yml`) | `ci/validate_krit.sh` |

Kotlin compiles incrementally in Bazel's persistent workers: after an edit, only the affected
sources of a module recompile. `bazel build --config=ic-summary //app` prints what each Kotlin
compilation did. [docs/bazel.md](docs/bazel.md) covers the Bazel setup, the module macros,
adding dependencies and modules, and the trade-offs behind them.

### Adding a module

Create the directory with the usual `src/main/kotlin` layout, then:

1. add a `BUILD.bazel` using `kt_jvm_module` or `kt_android_module` (copy a sibling's);
2. add a `build.gradle.kts` applying the matching convention plugin, and `include` it in
   `settings.gradle.kts`, so Android Studio sees it;
3. if it's a new top-level layer, give it a visibility rule in `bazel/defs.bzl`.

### Dependencies

Versions live in `MODULE.bazel` (Bazel) and `gradle/libs.versions.toml` (Android Studio); bump
both together. After changing Maven artifacts, repin with `bazel/scripts/repin.py`: it makes Bazel
resolve the same versions as Gradle, and CI checks that it's been run.
Dependabot opens update PRs for the Gradle catalog, Bazel modules and GitHub Actions.

## CI

[`.github/workflows/commit.yml`](.github/workflows/commit.yml) runs on every pull request and on
`main`, fanned out so each check reports as soon as it can:

| Job | What it checks |
|---|---|
| Build APK | `bazel build //app`, uploads the debug APK |
| Build Base APK | the same for the PR's base commit, for the APK diff |
| Build Test APK | `bazel build //app:instrumentation_tests_apk` |
| Unit Tests | `bazel test //...` (pull requests: `--build_tests_only`) |
| Module Graph Check | Bazel analysis of `//...` (layer visibility) and buildifier |
| Dependency Versions | Bazel resolves the same library versions as Gradle (`bazel/scripts/repin.py --check`) |
| Android Platform | the Gradle build (what Android Studio syncs) still configures |
| Krit | [Krit](https://github.com/kaeawc/krit) static analysis (Kotlin, Compose, manifest and resource rules) in a few seconds; findings not in `ci/krit-baseline.xml` fail the check and show as code scanning annotations |
| ktfmt, Validate XML, Validate Shell Scripts | formatting and static checks |
| Diff APK from Base | [Diffuse](https://github.com/JakeWharton/diffuse) report commented on the PR |
| Android UI Tests | the instrumentation tests on [emulator.wtf](https://emulator.wtf) |
| AutoMobile JUnit Tests | [AutoMobile](https://github.com/kaeawc/auto-mobile) device tests |

Every Bazel job restores the disk cache (action outputs) that the last `main` run saved, so a pull
request rebuilds only what it changed. On each push to `main` the Unit Tests job builds all of
`//...` and saves a fresh disk cache, pruned to the entries that build used; the repository cache
(downloads) is saved from `main` when `MODULE.bazel` changes. protoc comes prebuilt rather than
compiled from C++ sources. A new push to a pull request cancels its previous run. Jobs that need the `EW_API_TOKEN` secret skip themselves where it isn't available
(Dependabot runs, forks).

## Build and IDE tuning notes

These notes cover the JVM, Gradle and Android Studio settings this repo uses, and why.

### JVM args

#### -XX:+UseG1GC

Every project's performance on G1GC vs ParallelGC seems to have slightly or significant characteristics. It is impossible to reliably test these algorithms with caching enabled due to the variances in network and IO bottlenecks, so I test these algorithms on clean builds with no caching. Android projects have a complicated memory footprint that can grow very quickly and as of JDK 17 most of the issues with G1GC have been fixed. This means that G1GC is the reliable option for returning memory and a good default for Android projects to stick with who aren't going to delve into JVM tuning. Also every JDK version since 17 has released iterations to improve upon G1GC to bring it closer and closer to Parallel's throughput performance levels. I still recommend testing GC algorithms on a case-by-case basis for JVM tuning.

#### -Xmx and -Xms

Since we're on the [GitHub actions free tier we have roughly 16GB of memory available](https://docs.github.com/en/actions/using-github-hosted-runners/using-github-hosted-runners/about-github-hosted-runners#standard-github-hosted-runners-for-public-repositories) in the worker. We therefore have plenty of resources available to us, but profiling shows we just don't use much in this build.

#### -XX:SoftRefLRUPolicyMSPerMB=1

Read my article about [SoftRefLRUPolicyMSPerMB in JVM Builds](https://www.jasonpearson.dev/softreflrupolicymspermb-in-jvm-builds/)

#### No Metaspace Settings

Read my article about [Metaspace in JVM Builds](https://www.jasonpearson.dev/metaspace-in-jvm-builds/) for my reasoning and approach metaspace.

#### -XX:ReservedCodeCacheSize

Read my articles about [CodeCache for JVM Builds](https://www.jasonpearson.dev/codecache-in-jvm-builds/) and [Kotlin JVM arg Inheritance & Defaults](https://www.jasonpearson.dev/kotlin-jvm-args-inheritance-and-defaults/).

#### -XX:+HeapDumpOnOutOfMemoryError

If your build does have an OOM and you want to analyze why it happened you're going to want the heap dump file. Of course you'd have to setup saving this file as a job artifact to make it accessible.

### Gradle properties

The full set lives in [gradle.properties](gradle.properties) with inline commentary. The
performance-relevant choices break down into three groups.

#### Caching

- `org.gradle.caching=true` — the local (and, if configured, remote) build cache. Every task with
  deterministic outputs for the same inputs is skipped on subsequent builds.
- `org.gradle.configuration-cache=true` with `configuration-cache.problems=warn` — caches the whole
  configuration phase, so an unchanged build graph skips configuration entirely.
- `org.gradle.configuration-cache.parallel=true` — loads config cache entries in parallel (Gradle
  8.11+).

#### Parallelism and isolation

- `org.gradle.parallel=true` — runs decoupled projects in parallel.
- `org.gradle.tooling.parallel=true` — turns on the IDE's parallel model fetch during sync
  (Android Studio Quail 1+; see [IDE Sync](#ide-sync)). Ignored by command-line builds.
- `org.gradle.unsafe.isolated-projects=true` — [Isolated Projects](https://docs.gradle.org/current/userguide/isolated_projects.html)
  lets each project configure and produce tooling models in parallel, cached and invalidated
  independently. This is the ceiling that most "parallel sync" advice is chasing, and it is the
  reason the module build files here avoid cross-project `subprojects {}`/`allprojects {}` wiring —
  those blocks are incompatible with it.
- `org.gradle.vfs.watch=true` — file-system watching keeps change detection cheap between builds.
- `kotlin.compiler.execution.strategy=daemon` and `kotlin.incremental=true` — daemon reuse and
  incremental Kotlin compilation.

#### Android / R8

- `android.nonTransitiveRClass=true` — each library's `R` class holds only its own resources,
  shrinking the generated `R` classes across a module graph.
- `android.enableBuildConfigAsBytecode=true` — skips the `BuildConfig` source-gen round trip for
  the one module (`:app`) that enables `BuildConfig` (off by default since AGP 9).
- `android.r8.maxWorkers=2` — bounds R8 parallelism so it coexists with parallel Gradle tasks
  without over-committing memory. R8 full mode is the AGP 9.0+ default.
- The disabled `buildfeatures` (resvalues, shaders) trim work this project never needs.

### Compiler flags

Kotlin compilation is configured once for every module: by the `androidbuild.kotlin-common`
convention plugin for Gradle, and by the toolchain in [bazel/kotlin](bazel/kotlin/BUILD.bazel)
for Bazel.

- `languageVersion` and `jvmTarget` are pinned from the version catalog
  ([gradle/libs.versions.toml](gradle/libs.versions.toml)) rather than the developer's default JDK,
  so `./gradlew` behaves identically across machines.
- The JDK and the bytecode level are separate catalog entries. `build-java-toolchain` (JDK 27, the
  newest release) runs the Gradle daemon ([gradle/gradle-daemon-jvm.properties](gradle/gradle-daemon-jvm.properties))
  and javac/kotlinc; the Foojay resolver downloads it when no local JDK 27 is installed.
  `build-java-target` (25) is the bytecode level javac and kotlinc emit, the same as the Bazel
  build's: rules_android's dexer (R8 9.3) rejects Java 26 class files. (Kotlin 2.4 tops out at
  JVM 26; JVM 27 arrives with Kotlin 2.5.)
- `freeCompilerArgs` uses `addAll` (not assignment) so plugin-contributed args — Compose, Metro —
  survive alongside the project's `-opt-in` list.
- `coreLibraryDesugaring` is enabled so the app can target a modern JDK while still running on the
  project's `minSdk`.

### Android Studio tuning

[studio.vmoptions](studio.vmoptions): I've included a sample file in this repo with some decent options. Since this is not the only project I work on with Android Studio I set my heap size a bit higher, but otherwise it matches the Gradle & Kotlin Daemon JVM args.

#### IDE sync

The single biggest sync win is **parallel model fetch**: the IDE fetches each project's Tooling
API model in parallel instead of serially. Block reported a ~57% reduction in sync duration from
this one switch in their [Shrinking Elephants](https://engineering.block.xyz/blog/shrinking-elephants)
writeup. How you turn it on has moved around: it began as an experimental IDE setting, then
piggy-backed on `org.gradle.parallel=true`, and since Android Studio Quail 1 (2026.1.2) it is its
own Gradle property, `org.gradle.tooling.parallel=true` in `gradle.properties` — which this repo
sets. (Before Quail 1 Patch 1 that property also required the build to be Isolated-Projects
compatible, which this one is; Patch 1 lifted the requirement.) It pairs naturally with the
Isolated Projects flag documented above.

Beyond that, sync cost scales with how many Gradle projects the IDE has to configure and how much
dependency resolution each one triggers. This repo measured project focusing (Spotlight),
pre-compiled artifact substitution (Artifact Swap), intransitive sync (Fastsync) and dependency
pre-fetching while Gradle was its only build; the write-ups and numbers are in
[docs/gradle-ide-sync-experiments.md](docs/gradle-ide-sync-experiments.md).

## License

[MIT](LICENSE)
