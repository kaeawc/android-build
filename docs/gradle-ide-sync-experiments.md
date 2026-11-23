# Gradle IDE-sync experiments (historical)

> These techniques ran in this repo while Gradle was its only build. They were retired when Bazel
> became the primary build and Gradle was reduced to the Android Studio build (see
> [bazel.md](bazel.md)); the write-ups and measurements are kept here because the findings still
> apply to large Gradle builds. File paths below refer to code that is now only in git history.

## Project focusing (Spotlight)

The [Spotlight](https://github.com/joshfriend/spotlight) Gradle plugin moves the project
`include`s out of [settings.gradle.kts](../settings.gradle.kts) into a flat
[gradle/all-projects.txt](../gradle/all-projects.txt) and computes the dependency graph by parsing
build scripts. To load only the projects you're working on into the IDE, list them in
`gradle/ide-projects.txt` (git-ignored, per-developer) and re-sync — Spotlight resolves their
transitive dependencies for you, so the IDE configures a focused subset instead of all 28 projects
(focusing `:feature:talks` brings in its 10-project closure; a simulated-sync cold configuration
drops from 1.5–2.3 s for the full graph to 0.85 s, best of three).
Install the companion [IDE plugin](https://plugins.jetbrains.com/plugin/27451-spotlight) to manage
the focus set from the UI. `./gradlew :checkAllProjectsList` guards that no stray `include`s creep
back into the settings file.

## Intransitive sync (Fastsync)

IDE code completion only needs each module's *compile* classpath, but the Gradle model the IDE
builds during sync also resolves every variant's *runtime* classpath — for a large graph, that is
most of the dependency-resolution work in a sync. Block's
[Fastsync](https://github.com/joshfriend/fastsync) trick is to make runtime classpaths
non-transitive during sync. This repo implements it as the `androidbuild.fastsync` convention
plugin ([build-logic](../build-logic/src/main/kotlin/androidbuild.fastsync.gradle.kts)), applied to
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

## Dependency pre-fetching

The last sync cost that none of the above touches is the network: the first sync on a freshly
checked-out branch still downloads whatever dependencies changed since the last one. Block's fix
is to warm the Gradle dependency cache in the background, from a git `post-checkout` hook and on
a schedule, so the sync finds everything already on disk. This repo's version:

- `./gradlew prefetchDependencies` — a task on every module (`androidbuild.prefetch`, applied via
  `androidbuild.kotlin-common`) whose only input is an artifact view of the *external* half of
  each compile and runtime classpath. Declaring those as task inputs makes Gradle download them
  (and run the AAR transforms) before the action runs; project dependencies are filtered out so
  nothing compiles. It has no outputs, so it always re-resolves, which is cheap once warm.
- [scripts/prefetch-dependencies.sh](../scripts/prefetch-dependencies.sh) runs it quietly with a
  `mkdir` lock so rapid branch hops don't stack Gradle invocations.
- [.githooks/post-checkout](../.githooks/post-checkout) runs the script in the background after
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
