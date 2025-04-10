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

TBD

## Compiler Flags

TBD

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
