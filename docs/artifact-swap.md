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
| Background artifact refresh on branch switch (opt-in) | `.githooks/post-checkout` |

The cycle: CI hashes every module's sources, publishes artifacts for changed modules at their
**content-hash version**, publishes a BOM (module → hash version), and fast-forwards the
`artifact-swap-green-main` branch. A developer's sync finds the newest BOM reachable from their
branch, downloads artifacts to Maven Local, and swaps every unchanged, unfocused module. Modules
with local changes (vs. the BOM commit) always stay real projects.

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

Not yet exercised: the CI publish pipeline against GitHub Packages end-to-end (`artifact-checker`
existence checks and BOM upload run for the first time on the first `main` push after this lands;
the Bearer-token auth and Maven-layout PUTs are the same calls the already-verified local publish
and PR-8 CI publish used).

## Known caveats

- GitHub Packages needs an authenticated token for every read; the gh CLI covers this after a
  one-time `gh auth refresh -s read:packages` (see setup).
- `artifactswap.enabled` defaults to **false** because an enabled sync with no BOM in Maven Local
  fails module selection ("found no matching BOMs"). Enable it after the first
  `download-artifacts.sh` run.
- Artifact Swap's settings plugin requires the Develocity plugin on the settings classpath (it
  references the build-scan API); it's applied without a server, so no scans are published.
- The CLI's telemetry endpoint is pointed at a fast-failing localhost port; the resulting log
  noise in CLI output is harmless.
