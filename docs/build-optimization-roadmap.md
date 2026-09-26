# Build Optimization Roadmap

This repo is adopting the IDE-sync and build-scaling techniques from Block's
[Shrinking Elephants](https://engineering.block.xyz/blog/shrinking-elephants) writeup. Most of
those techniques only pay off once the build is a real module graph, so the work is sequenced:
first grow a representative multi-module app, then layer the sync optimizations on top.

GitHub Issues are disabled on this repo, so this file is the durable home for the plan and each
step's acceptance criteria. Each item below ships as its own PR, merged and verified green on
`main` before the next begins.

## Target architecture

`app/build.gradle.kts` declares the graph via `moduleGraphAssert`:

```
:app          -> :*
:feature:*    -> :data:* , :client:* , :subsystem:* , :foundation:* , :core:*
:data:*       -> :client:* , :subsystem:* , :core:*
:client:*     -> :core:*
:subsystem:*  -> :foundation:* , :core:*
:foundation:* -> :foundation:* , :core:*
:core:*       -> :core:*
```

Rows 2–6b built the first 17 modules by porting them from the `auto-mobile/android/playground` app
onto this taxonomy and this repo's stack (Metro DI, navigation-compose, Compose BOM, convention
plugins). The jasonpearson.dev companion-app rebuild
([#437](https://github.com/kaeawc/android-build/pull/437)–[#443](https://github.com/kaeawc/android-build/pull/443))
then replaced the playground features and added the `:client:*` and `:data:*` layers, giving
28 modules today. Every mechanism below was carried through that rebuild unchanged.

## Sequence

| # | PR | Status | Acceptance |
|---|----|--------|-----------|
| 1 | Docs: IDE parallel model fetch + fill `Gradle Properties`/`Compiler Flags` | **merged** ([#405](https://github.com/kaeawc/android-build/pull/405)) | README sections filled; roadmap committed |
| 2 | `build-logic` included build + `androidbuild.kotlin-common` convention plugin | **merged** ([#406](https://github.com/kaeawc/android-build/pull/406)) | `:app` builds via the convention plugin; typesafe project accessors enabled (root renamed `android-build`); config cache + isolated projects still green |
| 3 | `:core:*` modules (`:core:common`, `:core:model`) + `androidbuild.kotlin-jvm` | **merged** ([#407](https://github.com/kaeawc/android-build/pull/407)) | Pure-Kotlin modules build and test; `:core:model → :core:common` edge; `assertModuleGraph` green |
| 4 | `:foundation:*` (`designassets`, `designsystem`, `navigation`) + `androidbuild.android-library`/`android-compose` | **merged** ([#408](https://github.com/kaeawc/android-build/pull/408)) | Compose theme + components, nav route contract; graph green |
| 5 | `:subsystem:*` (`analytics`, `storage`, `experimentation`) | **merged** ([#409](https://github.com/kaeawc/android-build/pull/409)) | Independent non-UI domains (rules forbid subsystem→subsystem); build + test; graph green |
| 6a | `:feature:*` ×8 (`login`, `home`, `discover`, `settings`, `mediaplayer`, `onboarding`, `slides`, `demos`) | **merged** ([#410](https://github.com/kaeawc/android-build/pull/410)) | Compose screens + pure UiState/reducers + tests; graph green |
| 6b | Wire `:app` NavHost → features | **merged** ([#411](https://github.com/kaeawc/android-build/pull/411)) | `:app` depends on all features; NavHost routes the `Destination` contract to feature screens (resume stays the launch start). Allowed `:foundation → :foundation` (designsystem now renders the designassets brand mark). |
| 7 | Adopt Spotlight (`com.fueledbycaffeine.spotlight` 1.7.0) | **merged** ([#412](https://github.com/kaeawc/android-build/pull/412)) | Settings plugin applied; `include`s moved to `gradle/all-projects.txt`; `:checkAllProjectsList` green; per-dev `gradle/ide-projects.txt` focuses IDE sync (gitignored); IDE plugin #27451 documented |
| 8 | GitHub Packages publishing for modules | **merged** ([#413](https://github.com/kaeawc/android-build/pull/413)) | Superseded by PR 9: the same convention plugin now applies Artifact Swap's content-hash publishing |
| 9 | Adopt artifact-swap (GitHub-backed) | **merged** ([#415](https://github.com/kaeawc/android-build/pull/415), [#417](https://github.com/kaeawc/android-build/pull/417), [#418](https://github.com/kaeawc/android-build/pull/418)) | Full functional swap on Kotlin DSL via hand-rolled swap-aware `projects.*` accessors — see [artifact-swap.md](artifact-swap.md). Sync excludes unchanged modules and resolves content-hash artifacts; locally-changed modules stay projects; CLI builds unaffected |
| 9a | Reduce Artifact Swap publish CI tax | **merged** ([#426](https://github.com/kaeawc/android-build/pull/426), [#428](https://github.com/kaeawc/android-build/pull/428)) | `ci-should-publish.sh` skips publishing when only non-swap-relevant files changed and leaves `artifact-swap-green-main` unchanged; `setup-gradle` caches the distribution and dependency caches for the two CLI-driven Gradle invocations. Pipeline 165 s -> 41 s on the first cached run; skip path exercised on the docs-only 4317699 (job 8 s, `skip: 1 changed file(s), none swap-relevant`); the advance push uses `GREEN_MAIN_TOKEN` (PAT) when a newer workflow edit has already landed on main, falling back to `GITHUB_TOKEN` otherwise |
| 10 | Fastsync / intransitive sync | **merged** ([#423](https://github.com/kaeawc/android-build/pull/423)) | `androidbuild.fastsync` convention plugin: every `*RuntimeClasspath` is non-transitive (and resolves consistently with its compile classpath) only when `idea.sync.active` is set; CLI/CI unchanged; `:app` sync-time runtime graph shrinks from 909 to 80 report lines with zero unresolved deps. See README "Intransitive sync" |
| 11 | Dependency pre-fetching | **merged** ([#424](https://github.com/kaeawc/android-build/pull/424)) | `prefetchDependencies` task (`androidbuild.prefetch`) resolves every module's external compile/runtime artifacts with no compilation; `scripts/prefetch-dependencies.sh` + opt-in (`prefetch.onCheckout=true`) background run from `.githooks/post-checkout` on branch switches, same contract as the Artifact Swap refresh. See README "Dependency pre-fetching" |

## Final state

With rows 10 and 11 merged, every technique from the Shrinking Elephants post has a working,
documented counterpart in this repo:

| Blog technique | Here |
|---|---|
| Modularization | 28 modules, layered graph, `assertModuleGraph` (rows 2–6b, then #437–#443) |
| Parallel configuration | Isolated Projects + configuration cache (`gradle.properties`) |
| Parallel model fetch | IDE setting, README "IDE Sync" (row 1) |
| Project focusing | Spotlight, `gradle/ide-projects.txt` (row 7) |
| Artifact substitution | Artifact Swap on GitHub Packages, [artifact-swap.md](artifact-swap.md) (rows 8–9) |
| Intransitive sync | `androidbuild.fastsync` (row 10) |
| Dependency pre-fetching | `prefetchDependencies` + `post-checkout` hook (row 11) |

The measured wins here are small by design: at 17 modules, and still at 28, the build is a
reference implementation of each mechanism, and every README section reports the real numbers
rather than the blog's. The 28-module re-measurement (README, [artifact-swap.md](artifact-swap.md))
shows the same pattern as before. Pre-fetching is the one clear local win (a 73% shorter first
resolve), Spotlight focusing roughly halves cold configuration, and Fastsync and Artifact Swap
shrink the graph without a wall-clock gain at this size.

Follow-ups surfaced by the re-measurement:

- [#444](https://github.com/kaeawc/android-build/pull/444) fixes an androidTest runtime classpath
  that Fastsync left unresolved.
- Artifact Swap content hashes ignore `gradle/libs.versions.toml` and `build-logic/`, so a
  dependency bump alone does not re-publish; see the caveat in [artifact-swap.md](artifact-swap.md).

## Infrastructure notes

- **artifact-swap without Artifactory.** artifact-swap needs a Maven repository to publish per-module
  artifacts to and resolve swaps from, plus a BOM "green-main" branch. This repo uses **GitHub
  Packages** (`maven.pkg.github.com/kaeawc/android-build`) as that Maven repo and a CI-maintained
  `artifact-swap-green-main` branch as the BOM tracker. GitHub Actions *run-artifacts* cannot serve
  as the swap source — they are run-scoped, ephemeral, and not a Maven layout.
- **Spotlight** (`com.fueledbycaffeine.spotlight`, github.com/joshfriend/spotlight) is OSS on Maven
  Central and a hard prerequisite for artifact-swap. IDE plugin: JetBrains Marketplace #27451.
- On a repo of this size the sync *speedup* from artifact-swap is a demonstration of the technique,
  not a production win — the payoff scales with hundreds of modules. Showcasing it end-to-end is the
  point.
