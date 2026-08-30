# Build Optimization Roadmap

This repo is adopting the IDE-sync and build-scaling techniques from Block's
[Shrinking Elephants](https://engineering.block.xyz/blog/shrinking-elephants) writeup. Most of
those techniques only pay off once the build is a real module graph, so the work is sequenced:
first grow a representative multi-module app, then layer the sync optimizations on top.

GitHub Issues are disabled on this repo, so this file is the durable home for the plan and each
step's acceptance criteria. Each item below ships as its own PR, merged and verified green on
`main` before the next begins.

## Target architecture

`app/build.gradle.kts` already declares the intended graph via `moduleGraphAssert`:

```
:app        -> :feature:* , :subsystem:* , :foundation:* , :core:*
:feature:*  -> :subsystem:* , :foundation:*
:subsystem:* -> :foundation:* , :core:*
:foundation:* -> :core:*
:core:*     -> :core:*
```

Modules are ported from the `auto-mobile/android/playground` app but mapped onto this taxonomy and
this repo's stack (Metro DI, navigation-compose, Compose BOM, convention plugins) rather than
playground's flat naming and `auto-mobile-sdk`/`navigation3` dependencies.

## Sequence

| # | PR | Status | Acceptance |
|---|----|--------|-----------|
| 1 | Docs: IDE parallel model fetch + fill `Gradle Properties`/`Compiler Flags` | **merged** ([#405](https://github.com/kaeawc/android-build/pull/405)) | README sections filled; roadmap committed |
| 2 | `build-logic` included build + `androidbuild.kotlin-common` convention plugin | **merged** ([#406](https://github.com/kaeawc/android-build/pull/406)) | `:app` builds via the convention plugin; typesafe project accessors enabled (root renamed `android-build`); config cache + isolated projects still green |
| 3 | `:core:*` modules (`:core:common`, `:core:model`) + `androidbuild.kotlin-jvm` | **merged** ([#407](https://github.com/kaeawc/android-build/pull/407)) | Pure-Kotlin modules build and test; `:core:model → :core:common` edge; `assertModuleGraph` green |
| 4 | `:foundation:*` (`designassets`, `designsystem`, `navigation`) + `androidbuild.android-library`/`android-compose` | **merged** ([#408](https://github.com/kaeawc/android-build/pull/408)) | Compose theme + components, nav route contract; graph green |
| 5 | `:subsystem:*` (`analytics`, `storage`, `experimentation`) | **in progress** | Independent non-UI domains (rules forbid subsystem→subsystem); build + test; graph green |
| 6 | `:feature:*` modules + wire `:app` | planned | Ported playground features build and are reachable from `:app`; UI/unit tests green |
| 7 | Adopt Spotlight (`com.fueledbycaffeine.spotlight`) | planned | Settings plugin applied; project focusing works; IDE plugin documented |
| 8 | GitHub Packages publishing for modules | planned | `maven-publish` pushes module artifacts to GitHub Packages from CI on green `main` |
| 9 | Adopt artifact-swap (GitHub-backed) | planned | `xyz.block.artifactswap` wired to GitHub Packages + `artifact-swap-green-main` BOM branch + post-checkout hook + CLI |
| 10 | Fastsync / intransitive sync | planned | Runtime classpaths not resolved during IDE sync; compile classpath still complete |

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
