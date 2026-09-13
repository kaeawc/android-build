# Artifact Swap (GitHub-Packages-backed)

This repo adopts [Block's Artifact Swap](https://github.com/block/artifact-swap) — during IDE
sync, Gradle projects that are unchanged relative to the last green `main` are **excluded from the
build and replaced by pre-compiled artifacts**, so the IDE configures only the modules you're
actually working on. On this graph that means focusing one feature configures ~2 projects instead
of all 17; the payoff scales with module count.

Instead of Artifactory, artifacts live in **GitHub Packages**
(`maven.pkg.github.com/kaeawc/android-build`). Artifact Swap's repository client speaks plain
Maven-layout HTTP (GET/HEAD/PUT with a Bearer token), so GitHub Packages works as the backing
store; the notable difference is that GitHub Packages requires an authenticated token even to
*read* public packages.

## How the pieces fit

| Piece | Where |
|---|---|
| Settings plugin (`xyz.block.artifactswap.settings` 0.1.12, + Develocity + Spotlight `apply false`) | [settings.gradle.kts](../settings.gradle.kts) |
| Configuration (`artifactswap.*`) | [gradle.properties](../gradle.properties) |
| Swap-aware `projects.*` dependency accessors | `build-logic/src/main/kotlin/dev/jasonpearson/gradle/SwappableProjectDependencies.kt` |
| Publish plugin (content-hash versions), gated behind the CLI's version file | `build-logic/src/main/kotlin/androidbuild.publish.gradle.kts` |
| CLI install / artifact download / CI publish scripts | `scripts/artifact-swap/` |
| CI: publish artifacts + BOM, advance `artifact-swap-green-main` | [.github/workflows/publish.yml](../.github/workflows/publish.yml) |
| Background artifact refresh on branch switch (opt-in; the same hook also runs the opt-in dependency pre-fetch) | `.githooks/post-checkout` |

The cycle: after the Commit workflow goes green on `main`, CI first compares the commit with
`artifact-swap-green-main`. If only non-swap-relevant files changed (for example, docs or an
unrelated workflow), it skips the publish pipeline and leaves that branch unadvanced because the
BOM would be unchanged. Otherwise CI hashes every module's sources, publishes artifacts for
changed modules at their **content-hash version**, publishes a BOM (module → hash version), and
advances the `artifact-swap-green-main` branch (forward-only, serialized by a concurrency group,
and gated on the tests — so the branch really is green). The publish job also uses
`gradle/actions/setup-gradle@v4` to cache the Gradle distribution and dependency caches used by
the two CLI-driven Gradle invocations; the actual time saved depends on cache hits. A developer's
sync finds the newest BOM reachable from their branch, downloads artifacts to Maven Local, and
swaps every unchanged, unfocused module. Modules with local changes (vs. the BOM commit) always
stay real projects. The advance push uses the `GREEN_MAIN_TOKEN` repository secret (a fine-grained
PAT with Contents and Workflows read/write access, scoped to this repo only) because GitHub refuses
a `GITHUB_TOKEN`-authenticated push when the target commit's workflow files differ from main's
workflow files at push time. This occurs when a newer workflow change has landed on main before the
earlier run reaches its push step. The push clears the checkout-persisted Authorization header
(`http.https://github.com/.extraheader`) so the PAT is actually used instead of being silently
overridden by the persisted `GITHUB_TOKEN` header. Without the secret set, the step falls back to
`GITHUB_TOKEN` and only fails when this happens; a maintainer can then push the branch by hand
(`git push origin <sha>:refs/heads/artifact-swap-green-main`) to a commit whose BOM is already
published.

## Developer setup

1. Grant the GitHub CLI's token the packages scope (one-time):
   ```
   gh auth refresh -s read:packages
   ```
   `download-artifacts.sh` then mints the token file from `gh auth token` automatically — no
   manually-created PAT needed. (No `gh`? Export `SECRETS_PATH` to a directory containing
   `github-token.txt` with a `read:packages` PAT instead.)
2. `scripts/artifact-swap/download-artifacts.sh` — installs the CLI on first run and pulls the
   BOM + artifacts into `~/.m2`.
3. Set `artifactswap.enabled=true` in `gradle.properties` (or `-Partifactswap.enabled=true`).
4. List the projects you want to work on in `gradle/ide-projects.txt` (Spotlight focusing) and
   re-sync. The sync log shows `Using Artifact Swap!` and a module-selection summary.

Command-line builds never swap — the feature keys off `idea.sync.active`, so CI and terminal
builds always use real projects.

## The Kotlin DSL story (why `projects.*` is hand-rolled)

Three mechanisms were evaluated for declaring swappable inter-module dependencies from
`build.gradle.kts`; two cannot work:

1. **Gradle's generated type-safe accessors** (`TYPESAFE_PROJECT_ACCESSORS`) require every
   referenced project to exist in the build — Artifact Swap's whole point is to *exclude* swapped
   projects, so the generated accessors are structurally incompatible (upstream documents the same
   limitation and works around it with `artifact-swap-always-keep.txt`).
2. **`project(":path")` string notation** cannot be intercepted from Kotlin scripts: inside
   `dependencies {}` the call binds to Gradle's own Kotlin DSL extension ahead of any importable
   override — verified against build-logic `implementation`/`api` scopes, the root `buildscript`
   classpath, and explicit imports. This is also why Artifact Swap's bundled Kotlin
   `DependencyHandler.project` override (0.1.12) never engages (and it additionally looks up the
   BOM by project path against artifactId keys, and builds a version-only dependency from the
   match). Its Groovy metaprogramming override has no Kotlin equivalent.
3. **A hand-maintained `projects.*` accessor tree** (this repo's approach): an explicitly imported
   `DependencyHandler.projects` extension whose leaves return a normal project dependency when the
   swap is inactive, or the versionless `group:artifactId` notation when active — exactly what the
   Groovy override emits. Artifact Swap's `ArtifactSwapProjectPlugin` dependency substitution then
   pins the BOM version (project excluded) or swaps back to the real project (project present).
   The `projects.foo.bar` call-site *shape* is load-bearing: Spotlight discovers the project graph
   by statically parsing build scripts, and these call sites match its type-safe accessor pattern.

**When adding a module**, register it in `gradle/all-projects.txt` (or run
`./gradlew :fixAllProjectsList`) *and* add its accessor to `SwappableProjectDependencies.kt` —
a missing accessor is a compile error at first use, so it can't be forgotten silently.

## Verified behavior

Locally proven end-to-end (fresh clone, simulated sync via `-Didea.sync.active=true`):

- Focusing `:feature:home`: `1 selected out of 7 candidates … excluded: 6`; its compile classpath
  resolves `dev.jasonpearson.android:foundation_designsystem:<content-hash>` etc. from Maven
  Local, including transitive artifact→artifact edges (`core_common` via the artifact POM).
- Touching a file in `:foundation:navigation` and re-syncing: `2 selected … local changes: 1`,
  and the dependency resolves as `project ':foundation:navigation'` again.
- CLI builds (`assembleDebug`, `assertModuleGraph`, `checkAllProjectsList`, unit tests) are
  unaffected with the swap enabled or disabled.

The CI publish pipeline against GitHub Packages has now been proven end-to-end in CI: since PR
#418, commits a5bb1e7, f6d25c4, and e6f8705 each published a BOM
`dev.jasonpearson.android:bom:<sha>` to GitHub Packages and advanced the `artifact-swap-green-main`
marker/ref, and `artifact-checker` skipped already-published module hashes on the latest run. The
guard is covered by its standalone throwaway-repository test and workflow lint; the publish path and
Gradle cache behavior were exercised on commit 2c63735, and only the skip path is still
unexercised. Note the publish repository requires BOTH
`artifactswap.artifactRepo.username` and the token to attach credentials at all -- the username is
set in gradle.properties; removing it would silently publish unauthenticated and 401.

The first before-and-after publish measurements are:

| phase | before | after |
| --- | --- | --- |
| hashing | 54s | 15s |
| task-finder | 100s | 19s |
| artifact-checker | 4s | 3s |
| task-runner | — | 1s |
| bom-publisher | — | 4s |

The pipeline total dropped from 165s to 41s, and the whole job took about 80 seconds including
checkout, setup-gradle cache restore, and cache save. The after numbers come from one Publish run on
main for commit 2c63735 on 2026-09-13, with a warm dependency cache restored from the Commit
workflow's cache; this is a single-run measurement, not an average. The skip path is still
unexercised because no docs-only commit has landed since the guard merged to trigger a real skip
decision.

## Known caveats

- GitHub Packages needs an authenticated token for every read; the gh CLI covers this after a
  one-time `gh auth refresh -s read:packages` (see setup).
- `artifactswap.enabled` defaults to **false** because an enabled sync with no BOM in Maven Local
  fails module selection ("found no matching BOMs"). Enable it after the first
  `download-artifacts.sh` run.
- The CLI is a patched build of upstream v0.1.12 (GitHub Packages requires authentication on
  every request, and its generated maven-metadata.xml omits `<release>`); see
  [patches/artifact-swap/](../patches/artifact-swap/) for the diff and rebuild instructions. The
  Gradle plugins are stock.
- Artifact Swap's settings plugin requires the Develocity plugin on the settings classpath (it
  references the build-scan API); it's applied without a server, so no scans are published.
- The CLI's telemetry endpoint is pointed at a fast-failing localhost port; the resulting log
  noise in CLI output is harmless.
