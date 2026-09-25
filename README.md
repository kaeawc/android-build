[![Commit](https://github.com/kaeawc/android-ci/actions/workflows/commit.yml/badge.svg)](https://github.com/kaeawc/android-ci/actions/workflows/commit.yml)

# Android Build Experiments

This is a repository for experimenting with Android Build options. Different providers, build tools, and methods are used to showcase the different options available as well as best practices for workflow and performance. I have no intention of building an actual UX of any kind in this repository. This is where I catch and solve bugs that arise from the Android, Gradle, and Kotlin ecosystems or try out build concepts.

## JVM Args

### -XX:+UseG1GC

Every project's performance on G1GC vs ParallelGC seems to have slightly or significant characteristics. It is impossible to reliably test these algorithms with caching enabled due to the variances in network and IO bottlenecks, so I test these algorithms on clean builds with no caching. Android projects have a complicated memory footprint that can grow very quickly and as of JDK 17 most of the issues with G1GC have been fixed. This means that G1GC is the reliable option for returning memory and a good default for Android projects to stick with who aren't going to delve into JVM tuning. Also every JDK version since 17 has released iterations to improve upon G1GC to bring it closer and closer to Parallel's throughput performance levels. I still recommend testing GC algorithms on a case-by-case basis for JVM tuning.

### -Xmx and -Xms

Since we're on the [GitHub actions free tier we have roughly 16GB of memory available](https://docs.github.com/en/actions/using-github-hosted-runners/using-github-hosted-runners/about-github-hosted-runners#standard-github-hosted-runners-for-public-repositories) in the worker. We therefore have plenty of resources available to us, but profiling shows we just don't use much in this build.

### -XX:SoftRefLRUPolicyMSPerMB=1

Read my article about [SoftRefLRUPolicyMSPerMB in JVM Builds](https://www.jasonpearson.dev/softreflrupolicymspermb-in-jvm-builds/)

### No Metaspace Settings

Read my article about [Metaspace in JVM Builds](https://www.jasonpearson.dev/metaspace-in-jvm-builds/) for my reasoning and approach metaspace.

### -XX:ReservedCodeCacheSize

Read my articles about [CodeCache for JVM Builds](https://www.jasonpearson.dev/codecache-in-jvm-builds/) and [Kotlin JVM arg Inheritance & Defaults](https://www.jasonpearson.dev/kotlin-jvm-args-inheritance-and-defaults/).

### -XX:+HeapDumpOnOutOfMemoryError

If your build does have an OOM and you want to analyze why it happened you're going to want the heap dump file. Of course you'd have to setup saving this file as a job artifact to make it accessible.

## Gradle Properties

The full set lives in [gradle.properties](gradle.properties) with inline commentary. The
performance-relevant choices break down into three groups.

### Caching

- `org.gradle.caching=true` — the local (and, if configured, remote) build cache. Every task with
  deterministic outputs for the same inputs is skipped on subsequent builds.
- `org.gradle.configuration-cache=true` with `configuration-cache.problems=warn` — caches the whole
  configuration phase, so an unchanged build graph skips configuration entirely.
- `org.gradle.configuration-cache.parallel=true` — loads config cache entries in parallel (Gradle
  8.11+).

### Parallelism and isolation

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

### Android / R8

- `android.nonTransitiveRClass=true` — each library's `R` class holds only its own resources,
  shrinking the generated `R` classes across a module graph.
- `android.enableBuildConfigAsBytecode=true` with `buildFeatures.buildconfig=false` — skips the
  `BuildConfig` source-gen round trip.
- `android.r8.maxWorkers=2` — bounds R8 parallelism so it coexists with parallel Gradle tasks
  without over-committing memory. R8 full mode is the AGP 9.0+ default.
- `android.lint.useK2Uast=true` and the disabled `buildfeatures` (aidl, renderscript, resvalues,
  shaders) trim work this project never needs.

## Compiler Flags

Kotlin compilation is configured once per module (see [app/build.gradle.kts](app/build.gradle.kts),
and once the module graph lands, the `androidbuild.kotlin-common` convention plugin):

- `languageVersion` and `jvmTarget` are pinned from the version catalog
  ([gradle/libs.versions.toml](gradle/libs.versions.toml)) rather than the developer's default JDK,
  so `./gradlew` behaves identically across machines.
- `freeCompilerArgs` uses `addAll` (not assignment) so plugin-contributed args — Compose, Metro,
  `-Xcontext-parameters` — survive alongside the project's `-opt-in` list.
- `coreLibraryDesugaring` is enabled so the app can target a modern JDK while still running on the
  project's `minSdk`.

# CI Setup

This project includes a comprehensive CI setup that showcases typical automated checks with a focus on speed. I use a fan-in approach where it makes sense so the commit workflow on a PR quickly checks and provides as much feedback as possible on the change being tested. This is not overly resource intensive due to the combination of caching every part of the Gradle build process possible (build cache, dependency cache, and configuration cache) as well as the performance tuning. I'm also showing how to easily integrate with Emulator.wtf which is currently the fastest and most reliable Android UI test platform.

<img width="600" alt="Current CI Flow" src="https://github.com/user-attachments/assets/201b25ef-0d13-44f8-a784-00f8df85d409">

Build APK: Generates an artifact for debug build that could be shared within a development team and dependency for UI test job.

Build Base APK: Checks out source from base commit and builds a debug build artifact.

Build Test APK: Dependency for UI test job, no artifact.

Unit Tests: Regular Android JVM Unit tests.

Spotless: Performs all configured Spotless plugin checks. This mostly validates that copyright headers have been applied to source files.

Module Graph: Validates the module graph, checks that it adheres to the existing rules and limits the depth of the graph.

Android Lint: Runs an Android Lint check on the Release variant. Exports in HTML and SARIF formats.

Android UI Tests: Runs Build & Test APKs on Emulator.wtf.

Diff APK from Base: Uses Diffuse against the current and base APK artifacts and comments on the relevant PR.

## Android Studio

[studio.vmoptions](studio.vmoptions): I've included a sample file in this repo with some decent options. Since this is not the only project I work on with Android Studio I set my heap size a bit higher, but otherwise it matches the Gradle & Kotlin Daemon JVM args.

### IDE Sync

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
dependency resolution each one triggers. The techniques for cutting that down at scale — project
focusing, pre-compiled artifact substitution, and intransitive sync — are all adopted in this repo
and tracked in [docs/build-optimization-roadmap.md](docs/build-optimization-roadmap.md). Artifact
substitution is live: see [docs/artifact-swap.md](docs/artifact-swap.md) for how unchanged modules
are swapped for pre-compiled GitHub-Packages artifacts during IDE sync, and what that took on a
Kotlin-DSL build.

#### Project focusing (Spotlight)

The [Spotlight](https://github.com/joshfriend/spotlight) Gradle plugin moves the project
`include`s out of [settings.gradle.kts](settings.gradle.kts) into a flat
[gradle/all-projects.txt](gradle/all-projects.txt) and computes the dependency graph by parsing
build scripts. To load only the projects you're working on into the IDE, list them in
`gradle/ide-projects.txt` (git-ignored, per-developer) and re-sync — Spotlight resolves their
transitive dependencies for you, so the IDE configures a focused subset instead of all 28 projects
(focusing `:feature:talks` brings in its 10-project closure; a simulated-sync cold configuration
drops from 1.5–2.3 s for the full graph to 0.85 s, best of three).
Install the companion [IDE plugin](https://plugins.jetbrains.com/plugin/27451-spotlight) to manage
the focus set from the UI. `./gradlew :checkAllProjectsList` guards that no stray `include`s creep
back into the settings file.

#### Intransitive sync (Fastsync)

IDE code completion only needs each module's *compile* classpath, but the Gradle model the IDE
builds during sync also resolves every variant's *runtime* classpath — for a large graph, that is
most of the dependency-resolution work in a sync. Block's
[Fastsync](https://github.com/joshfriend/fastsync) trick is to make runtime classpaths
non-transitive during sync. This repo implements it as the `androidbuild.fastsync` convention
plugin ([build-logic](build-logic/src/main/kotlin/androidbuild.fastsync.gradle.kts)), applied to
every module through `androidbuild.kotlin-common`: when the `idea.sync.active` system property is
set, every resolvable `*RuntimeClasspath` configuration is marked `isTransitive = false` and
resolved consistently with its `*CompileClasspath` twin (which keeps BOM-managed versions intact).
It matches by name suffix rather than walking the JVM `SourceSetContainer` the way the upstream
plugin does, because AGP's variant classpaths (`debugRuntimeClasspath`,
`debugUnitTestRuntimeClasspath`, …) never appear in that container.

Command-line builds and CI are byte-identical — nothing keys off anything but the sync property.
Simulate a sync from the terminal with `-Didea.sync.active=true`; opt out for a session with
`-Pfastsync.enabled=false` (Compose previews and the debugger use the runtime classpath, so if one
of those stops finding a transitively-provided class, that is the switch). Measured on this repo
(normal clone, simulated sync, best of three; "cold" is `--no-configuration-cache`):

| Simulated sync (`-Didea.sync.active=true`) | 17 modules: on | off | 28 modules: on | off |
|---|---|---|---|---|
| Cold configuration (`help`) | 1.5 s | 1.5 s | 1.50 s | 1.65 s |
| Warm (configuration-cache hit) | 0.9 s | 0.9 s | 1.15 s | 0.78 s |
| Every module's `:dependencies` (resolves all configurations) | — | — | 4.59 s | 3.92 s |
| `:app` `debugRuntimeClasspath` report, lines | 80 | 909 | 164 | 1,670 |
| All-modules dependency report, lines | — | — | 89,616 | 132,943 |

The graph shrinks exactly as intended: at 28 modules the whole-build report is a third smaller and
`:app`'s runtime classpath is a tenth the size. It stays fully resolved too, with zero `FAILED`
entries, including under an Artifact-Swap-active sync where swapped modules resolve to
content-hash artifacts. The 28-module run is what caught the androidTest case:
[#444](https://github.com/kaeawc/android-build/pull/444) fixed a test-only BOM-managed dependency
that AGP had left unresolved. The wall-clock win is still zero. The warm and all-modules rows are
sub-second noise in both directions, and with a warm dependency cache, resolution is not where
sync time goes at this size. Block's 94% came from thousands of modules where it is. This repo is
the reference implementation of the mechanism; the honest numbers are the point. (These local
runs had Isolated Projects off via a machine-wide `~/.gradle/gradle.properties` override; the
repo's own setting is on.)

#### Dependency pre-fetching

The last sync cost that none of the above touches is the network: the first sync on a freshly
checked-out branch still downloads whatever dependencies changed since the last one. Block's fix
is to warm the Gradle dependency cache in the background, from a git `post-checkout` hook and on
a schedule, so the sync finds everything already on disk. This repo's version:

- `./gradlew prefetchDependencies` — a task on every module (`androidbuild.prefetch`, applied via
  `androidbuild.kotlin-common`) whose only input is an artifact view of the *external* half of
  each compile and runtime classpath. Declaring those as task inputs makes Gradle download them
  (and run the AAR transforms) before the action runs; project dependencies are filtered out so
  nothing compiles. It has no outputs, so it always re-resolves, which is cheap once warm.
- [scripts/prefetch-dependencies.sh](scripts/prefetch-dependencies.sh) runs it quietly with a
  `mkdir` lock so rapid branch hops don't stack Gradle invocations.
- [.githooks/post-checkout](.githooks/post-checkout) runs the script in the background after
  *branch* checkouts only (the `$3 = 1` contract the Artifact Swap refresh already uses), when
  `prefetch.onCheckout=true` is set in `gradle.properties` (repo or `~/.gradle`). It is opt-in and
  off by default; CI restores its dependency cache instead.

Measured on this repo (fresh `GRADLE_USER_HOME`, wrapper and plugins already downloaded, so the
numbers isolate `modules-2`):

| Fresh `modules-2`, then… | 17 modules: without | with | 28 modules: without | with |
|---|---|---|---|---|
| `prefetchDependencies` (background, cold) | — | 60.8 s (+134 MB) | — | 78.7 s (+126 MB) |
| First `:app:dependencies` on the branch (resolves every `:app` configuration) | 92.7 s | 24.4 s | 131.2 s | 35.6 s |
| Warm re-run | 1.1 s | 1.1 s | 3–18 s | 3–19 s |

A 74% cut in the first-resolve wait at 17 modules and 73% at 28, in the same ballpark as Block's
83% (theirs is Develocity telemetry across a fleet; each of these is one machine, one run). At 28
modules the warm re-runs vary with daemon start-up and are the same with or without prefetch. The
28-module runs had Isolated Projects off, to match the other local measurements. The ~30 s that remains is what the proxy
resolves beyond the compile/runtime classpaths: lint and Kotlin-compiler classpaths, annotation
processors, and the report's own metadata fetches. On this repo the dependency set is small and CI
already restores its cache, so the value here is the pattern: the task, the lock-guarded script, and
the hook contract are what transfer to a build where a branch switch really does mean minutes of
downloads.
