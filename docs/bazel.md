# Bazel build

Bazel builds and tests every module and the app; CI runs it in [`.github/workflows/bazel.yml`](../.github/workflows/bazel.yml).
The Gradle build stays for Android Studio, which syncs Gradle projects. Both builds read the same
sources (the Gradle `src/main/kotlin`, `src/test/kotlin`, `src/main/res` layout).

## Requirements

- [Bazelisk](https://github.com/bazelbuild/bazelisk) installed as `bazel`. It reads `.bazelversion` (Bazel 9.3.3).
- An Android SDK in `$ANDROID_HOME` with `platforms;android-37.2` and `build-tools;37.0.0`.
- No JDK: Bazel downloads Temurin 27 itself (`//bazel/jdk`).

## Commands

| What | Command |
|---|---|
| Debug APK | `bazel build //app` → `bazel-bin/app/app.apk` |
| Release APK (R8, resource shrinking, `BuildConfig.DEBUG = false`) | `bazel build -c opt //app` |
| Install on a device | `bazel mobile-install //app` or `adb install bazel-bin/app/app.apk` |
| Instrumentation test APK | `bazel build //app:instrumentation_tests_apk` |
| All unit tests | `bazel test //...` |
| One module's tests | `bazel test //data/articles:tests` |
| One test class | `bazel test //data/articles:CacheModelsTest` |
| Everything CI builds | `bazel build //... && bazel test //...` |

Put machine-specific settings (cache locations, `--jobs`, worker limits) in `user.bazelrc` at the
repository root. It's git-ignored and imported by `.bazelrc`.

## Layout

| Path | What |
|---|---|
| `MODULE.bazel` | Bazel modules, JDK 27, Kotlin 2.4.21, the Android SDK, and Maven artifacts |
| `maven_install.json` | Lock file for `@maven` |
| `bazel/defs.bzl` | Module macros: `kt_jvm_module`, `kt_android_module`, `kt_android_application`, the Bazel counterparts of `build-logic`'s convention plugins |
| `bazel/kotlin/` | Kotlin toolchain (language 2.4, Java 25 bytecode, project-wide opt-ins) and compiler plugins (Compose, kotlinx.serialization, Metro) |
| `bazel/jdk/` | Temurin 27 as `remotejdk_27`, and the Java compile toolchain that runs javac on it |
| `bazel/scripts/align_androidx.py` | Repins `@maven` and aligns AndroidX versions (see below) |
| `third_party/okhttp/` | Picks OkHttp's JVM or Android artifact per target |
| `<module>/BUILD.bazel` | One per module: its dependencies and compiler plugins |

A module's `BUILD.bazel` mirrors its `build.gradle.kts`. For example:

```starlark
load("//bazel:defs.bzl", "kt_android_module")

kt_android_module(
    custom_package = "dev.jasonpearson.android.feature.photography",  # android.namespace
    compose = True,                                                  # androidbuild.android-compose
    plugins = ["metro"],                                             # alias(libs.plugins.metro)
    deps = [                                                         # implementation(...)
        "//data/photography",
        "@maven//:dev_chrisbanes_haze_haze_android",
    ],
)
```

`api(...)` dependencies go in `exports`, `testImplementation(...)` ones in `test_deps`. The library
target is named after the directory (`//feature/photography`), and `:tests` runs every `*Test.kt`.

## Kotlin incremental compilation

Kotlin compiles in persistent workers through the Kotlin Build Tools API, and the workers compile
incrementally: after an edit, a target recompiles only the affected sources instead of all of
them. `.bazelrc` turns this on:

```
build --@rules_kotlin//kotlin/settings:experimental_build_tools_api=true
build --worker_extra_flag=KotlinCompile=--experimental_incremental_compilation
```

The first compilation of a target is a plain one. Its next compilation sets up the incremental
state, and every compilation after that is incremental. To see what each compilation did, build
with `--config=ic-summary`:

```
bazel build --config=ic-summary //feature/photography
```

Worker flags aren't part of action keys, so incremental and clean builds share disk and remote
cache entries. CI compiles each target once, so it never creates incremental state.

This needs a rules_kotlin that isn't released yet. `MODULE.bazel` builds rules_kotlin from the
incremental compilation pull request stack ([0xLucasMarcal/rules_kotlin#1](https://github.com/0xLucasMarcal/rules_kotlin/pull/1),
headed for bazel-contrib/rules_kotlin) with `archive_override`, pinned to a commit. Once a
rules_kotlin release includes it, delete the `archive_override`.

## JDK and bytecode level

- **JDK 27** (Temurin) runs every tool: kotlinc, javac, R8/D8 and the Android resource tools. It's
  hermetic: `MODULE.bazel` downloads it through `//bazel/jdk:extensions.bzl` because rules_java's
  remote JDKs stop at 25.
- **Java 25 bytecode.** rules_android's dexer (R8 9.3) rejects Java 26 class files, so Bazel and
  Gradle (`build-java-target`) both emit Java 25. Kotlin 2.4 itself could go to 26.

## Dependencies

Maven artifacts are listed in `MODULE.bazel` and pinned in `maven_install.json`. The versions
match `gradle/libs.versions.toml`; bump both together.

**After changing artifacts, repin** (not `bazel run @maven//:pin`; see the alignment note below):

```
bazel/scripts/align_androidx.py
```

Notes on how dependencies resolve differently from Gradle:

- **Kotlin Multiplatform libraries.** Gradle picks each library's Android or JVM variant from
  Gradle module metadata. Bazel resolves plain POMs, so libraries whose root artifact holds only
  common metadata are listed by variant: `coil-compose-android`, `haze-android`,
  `metro:runtime-jvm`, `kotlinx-datetime-jvm`. AndroidX and most kotlinx roots already depend on
  the right variant.
- **OkHttp** publishes `okhttp-jvm` and `okhttp-android` with one API. Modules compile against
  `//third_party/okhttp` (not packaged), JVM unit tests run with `okhttp-jvm`, and the app packages
  `okhttp-android`.
- **AndroidX version alignment.** AndroidX POMs pin sibling artifacts to exact versions
  (`[1.7.0,1.7.0]`). Gradle reconciles those with dependency constraints; rules_jvm_external's
  Maven resolver fails on them. `align_androidx.py` repins and, for each conflict, adds the
  highest requested version to `ANDROIDX_ALIGNMENT` in `MODULE.bazel`.
- **Unit tests of Android modules** run on the plain JVM against `android.jar`, like Gradle's
  local unit tests without Robolectric.

## Not covered by Bazel

- **AutoMobile tests** (`app/src/test/.../automobiletest`) drive a device through the AutoMobile
  daemon; CI runs them with Gradle.
- **Android Lint** runs on the Gradle build.
- **Android Studio** syncs the Gradle build. The [Bazel plugin for IntelliJ](https://github.com/bazelbuild/intellij)
  can import this workspace too, but isn't set up here.
