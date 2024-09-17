[![Commit](https://github.com/kaeawc/android-ci/actions/workflows/commit.yml/badge.svg)](https://github.com/kaeawc/android-ci/actions/workflows/commit.yml)

# Android CI Experiments

This is a repository for experimenting with Android CI. Different providers, build tools, and
methods are used to showcase the different options available as well as best practices for
workflow and performance.

## JVM Args

### -XX:+UseG1GC

Every project's performance on G1GC vs ParallelGC seems to have slightly or significant characteristics. It is impossible to reliably test these algorithms with caching enabled due to the variances in network and IO bottlenecks, so I test these algorithms on clean builds with no caching. As of JDK 20 I started to see G1GC generally perform better than Parallel on significantly large Android projects, and every JDK version since has included significant achievements for G1GC. I still recommend testing GC algorithms on a per-project basis for fine tuning, but if you're hitting memory limits on your hardware this is the better option.

### -Xmx3g and -Xms3g

Since we're on the GitHub actions free tier we have roughly 6GB of memory available in the worker. We therefore can allocate approximately 3GB to the Gradle nad Kotlin daemons each, which is more than plenty for this size project.

### -XX:SoftRefLRUPolicyMSPerMB=1

This property defines how fast soft references can be evicted by the JVM. As they are soft references, if they are evicted the program that depends on them will just have to spend time and resources recreating them. Gradle and Kotlin daemons create a ton of these and the default `1000` means that with a typical Android memory heap of 2GB-8GB soft references don't get released until 33-133 minutes. Using a value of `1` changes this to 2-8 seconds for the same memory heap. If your CI run is that long you've got other problems, but allowing memory to get freed up is a significant resource clawback. I've observed a 10-30% peak memory reduction on large projects without any issues across Kotlin, Dagger, KSP, Compose, Room, etc.

### -XX:MetaspaceSize=1g

Read my article about [Metaspace in JVM Builds](https://www.jasonpearson.dev/metaspace-in-jvm-builds/) for my reasoning and approach to sizing metaspace.

### -XX:ReservedCodeCacheSize=256m -XX:CodeCacheExpansionSize=1m -XX:InitialCodeCacheSize=64m

CodeCache is where compiled native method and non-method code is cached in memory. Since JVM 11 this has been split into 3 distinct parts for performance reasons, but most applications shouldn't be concerned with tuning those individually. Instead we want to ensure the CodeCache is allowed to grow to make loading and use of classes easy to cache if we have the system resources for it - or constrain it so cache flushing occurs more often in a memory-constrained environment.

### -XX:+HeapDumpOnOutOfMemoryError

If your build does have an OOM and you want to analyze why it happened you're going to want the heap dump file. Of course you'd have to setup saving this file as a job artifact to make it accessible.

## Gradle Properties

TBD

## CI Setup

This project includes a comprehensive CI setup that showcases typical automated checks with a focus on speed. I use a fan-in approach for the commit workflow to quickly check and provide as much feedback as possible on the change being tested. This is not overly resource intensive due to the combination of caching every part of the Gradle build process possible (build cache, dependency cache, and configuration cache). I'm also showing how to easily integrate with Emulator.wtf which is currently the fastest and most reliable Android UI test platform.

<img width="631" alt="Screenshot 2024-08-31 at 5 28 28 AM" src="https://github.com/user-attachments/assets/4ce3f60f-1cbb-4f39-9eeb-779bb46da786">

Build APK: Generates an artifact for debug build that could be shared within a development team and dependency for UI test job.

Build Test APK: Dependency for UI test job, no artifact

Unit Tests: Regular Android JVM Unit tests

Spotless: Performs all configured Spotless plugin checks with ktfmt and spotless. This validates code format and ensures expected precommit checks have already been run.

Module Graph: Validates the module graph, checks that it adheres to the existing rules and limits the depth of the graph.

Android Lint: Runs an Android Lint check on the Release variant.
