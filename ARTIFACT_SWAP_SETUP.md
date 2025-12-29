# Artifact Swap Setup Guide

Complete documentation for setting up the artifact-swap plugin in this repository for faster Gradle sync times.

## Table of Contents

- [Overview](#overview)
- [Prerequisites](#prerequisites)
- [Initial Configuration](#initial-configuration)
- [Issues Encountered and Fixes](#issues-encountered-and-fixes)
- [Publishing Modules](#publishing-modules)
- [Creating the BOM](#creating-the-bom)
- [Verification](#verification)
- [Daily Workflow](#daily-workflow)
- [Troubleshooting](#troubleshooting)

---

## Overview

**Artifact Swap** is a Gradle plugin that dramatically improves IDE sync performance by:
1. **Swapping Gradle projects with pre-published Maven artifacts** during IDE sync
2. **Reducing configuration time** by only including projects you're actively working on
3. **Using content-based versioning** (SHA256 hashes) to ensure consistency

### Key Concepts

- **BOM (Bill of Materials)**: A special Maven POM file that lists all available artifact versions
- **Content-based versioning**: Each module version is a SHA256 hash of its content
- **Shared commits**: Git commits that exist both locally and on the remote branch
- **Local Maven repository**: `~/.m2/repository` - where artifacts are stored locally

### Performance Benefits

- **Without artifact swap**: All 11 projects configured every sync
- **With artifact swap**: Only active projects configured, others loaded as artifacts
- **Result**: Significantly faster Gradle sync times

---

## Prerequisites

### Repository Setup

This repository already has:
- ✅ Artifact-swap plugin as an included build: `../../github/artifact-swap`
- ✅ Settings plugin configured in `settings.gradle.kts`
- ✅ Artifact version hash file: `artifact-sync-hash-output`
- ✅ CLI tool built: `/Users/jason/github/artifact-swap/artifactswap-shadow-0.1.5-SNAPSHOT/`

### Required Files

1. **settings.gradle.kts** - Plugin configuration
2. **gradle.properties** - Feature flags and configuration
3. **artifact-sync-hash-output** - Content hashes for each module
4. **build.gradle.kts** - Root build configuration

---

## Initial Configuration

### 1. Settings Configuration (`settings.gradle.kts`)

The artifact-swap plugin is configured in `settings.gradle.kts`:

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://storage.googleapis.com/r8-releases/raw") { name = "R8-releases" }
    }

    // Include the artifact-swap plugin as an included build
    includeBuild("../../github/artifact-swap")
}

// Load AGP, KGP, and R8 in settings classpath so the artifact-swap plugin can detect/use them
buildscript {
    repositories {
        google()
        mavenCentral()
        maven("https://storage.googleapis.com/r8-releases/raw") { name = "R8-releases" }
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.13.2")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.0")
        classpath("com.android.tools:r8:8.13.19")
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

// Apply the artifact-swap settings plugin
plugins { 
    id("xyz.block.artifactswap.settings") version "0.1.4-SNAPSHOT" 
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "android-build"

// Include all projects
include(":app")
include(":analytics")
include(":design:assets")
include(":design:system")
include(":experimentation")
include(":home")
include(":login")
include(":mediaplayer")
include(":onboarding")
include(":settings")
include(":storage")
```

**Critical Details:**
- AGP and KGP **must be in the buildscript classpath** before the plugin is applied
- The plugin version is `0.1.4-SNAPSHOT` (from the included build)
- The included build path is relative: `../../github/artifact-swap`

### 2. Gradle Properties Configuration (`gradle.properties`)

Add these properties to `gradle.properties`:

```properties
# Artifact Swap Configuration
artifactswap.enabled=true
artifactswap.publishingEnabled=true
artifactswap.primaryRepositoryName=local
artifactswap.primaryArtifactsMavenGroup=dev.jasonpearson.android
artifactswap.bomSourceBranchName=fix-build-script-order
artifactswap.artifactVersionFile=artifact-sync-hash-output
artifactswap.artifactRepo.url=file:///Users/jason/.m2/repository
artifactswap.artifactoryBaseUrl=https://artifactory.example.com
artifactswap.artifactoryPublisherTokenFileName=test-token.txt
```

**Property Explanations:**

- `artifactswap.enabled` - Activates artifact swap during IDE sync
- `artifactswap.publishingEnabled` - Enables publishing tasks for modules
- `artifactswap.primaryRepositoryName` - Name of the Maven repository (for Artifactory, use "local" for local Maven)
- `artifactswap.primaryArtifactsMavenGroup` - Maven group ID for published artifacts
- `artifactswap.bomSourceBranchName` - Git branch name where BOMs are tracked (YOUR current branch)
- `artifactswap.artifactVersionFile` - File containing SHA256 hashes for each module
- `artifactswap.artifactRepo.url` - Local Maven repository URL (use `file://` protocol)
- `artifactswap.artifactoryBaseUrl` - Artifactory base URL (used for analytics, can be a dummy URL)
- `artifactswap.artifactoryPublisherTokenFileName` - Token file name (for CI publishing, not needed for local)

### 3. Root Build Configuration (`build.gradle.kts`)

**CRITICAL FIX**: Comment out Android/Kotlin plugin declarations in `build.gradle.kts`:

```kotlin
plugins {
    `version-catalog`
    alias(libs.plugins.dependencyAnalysis)
    // Kotlin/Android plugins are already loaded in buildscript classpath in settings.gradle.kts
    // for artifact-swap plugin compatibility, so we can't redeclare them here
//    alias(libs.plugins.kotlin.android) apply false
//    alias(libs.plugins.android.library) apply false
//    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.metro) apply false
}
```

**Why this is necessary:**
- The plugins are already loaded in the settings buildscript classpath
- Redeclaring them causes a version conflict: "plugin is already on the classpath with an unknown version"
- The artifact-swap plugin needs to detect AGP/KGP versions from the buildscript classpath

### 4. Module Build Files

Individual module build files apply plugins by ID instead of using the version catalog:

```kotlin
// analytics/build.gradle.kts (example)
plugins {
    // AGP and KGP are loaded via settings buildscript
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "dev.jasonpearson.analytics"
    compileSdk = 36
    // ... rest of configuration
}
```

**Note:** The artifact-swap plugin automatically applies `maven-publish` plugin to all modules when `artifactswap.publishingEnabled=true`.

---

## Issues Encountered and Fixes

### Issue 1: Plugin Version Conflict

**Error:**
```
Error resolving plugin [id: 'org.jetbrains.kotlin.android', version: '2.3.0', apply: false]
> The request for this plugin could not be satisfied because the plugin is already 
  on the classpath with an unknown version, so compatibility cannot be checked.
```

**Root Cause:**
- AGP and KGP were loaded in settings buildscript classpath
- Then redeclared in root `build.gradle.kts` plugins block
- Gradle doesn't allow the same plugin twice with potentially different versions

**Fix:**
Comment out the plugin declarations in `build.gradle.kts`:
```kotlin
//    alias(libs.plugins.kotlin.android) apply false
//    alias(libs.plugins.android.library) apply false
//    alias(libs.plugins.android.application) apply false
```

### Issue 2: BOM Not Found - "Traversed 5000 commits"

**Error:**
```
xyz.block.artifactswap.core.module_selector.RealArtifactSwapModuleSelector$ModuleSelectorException$FailedDeterminingBomVersionException: 
Failed to determine BOM version
Caused by: java.lang.IllegalStateException: 
Traversed 5000 commits from origin/fix-build-script-order and found no matching BOMs
```

**Root Cause:**
The artifact-swap plugin searches git history for **"shared commits"** (commits that exist both locally and on the remote branch) and checks if a BOM exists for those commits. The search failed because:
1. No BOMs had been published yet
2. Local commits weren't pushed to origin, so they weren't "shared"

**Understanding Shared Commits:**
- Plugin uses `git log origin/fix-build-script-order..HEAD` to find shared commits
- Only commits that exist on **both** local and remote branches are "shared"
- If you have unpushed local commits, they won't be found

**Fix:**
1. Find the merge-base commit (guaranteed to be shared):
   ```bash
   git merge-base HEAD origin/fix-build-script-order
   # Output: c8a2529cc49d66bb736a891410fd019e10869009
   ```

2. Publish a BOM for that commit (see "Creating the BOM" section)

3. Alternative: Push your local commits to make them "shared":
   ```bash
   git push origin fix-build-script-order
   ```

### Issue 3: Publishing Tasks Not Available

**Error:**
```
Cannot locate tasks that match ':analytics:publishToArtifactSwapRepository' 
as task 'publishToArtifactSwapRepository' not found
```

**Root Cause:**
Publishing tasks are only created when **both** conditions are met:
- `artifactswap.enabled=true` (activates plugin during IDE sync)
- `artifactswap.publishingEnabled=true` (creates publishing tasks)

If you set `artifactswap.enabled=false` to work around the BOM error, publishing tasks disappear.

**Fix:**
Keep both properties set to `true`:
```properties
artifactswap.enabled=true
artifactswap.publishingEnabled=true
```

And fix the BOM issue instead (see Issue 2).

---

## Publishing Modules

### Understanding Module Versions

Each module version is the **SHA256 hash of its content**, stored in `artifact-sync-hash-output`:

```
:analytics|75FA01205D25C402F5AE5469CD62AC44832B7658F4503AAA36D9A7CF33C8170D
:app|2FB77423E2C380DAE0E5AF3DF0CE58589301DABE4F06B2382B528C76CE3BD528
:design:assets|7A0C12E42CC26403AE99E6C7402390CEFDD51810909A112089F2A07B3B8F9DDE
:design:system|77BB164B9B7B081F66CD6C5AA50CCF430EB7A214762F1EA78616171C3CE57B98
:experimentation|390F9E43033610EA9450AEDDA768C6A25EA61BEEF6DB037F140517FFCDD6E550
:home|13B5E5A97B5D79783B1E9EF0E0B7BFEC7CE2D208317B338B68ABB1834D3A3D0D
:login|20B3A92F642B22C7FE57B7DA5E6ADDD9CB5B4C3BCCA512399DC04A28F6F60DE9
:mediaplayer|59BDC717B4A53AB8754B69716FE61D6586FF471CD5F76A7AEEB77C1768F3BD58
:onboarding|C63FAD4B7411F79B4A1FC88FE9D9041DDCA85E5BCCEBB5EC0D294C8DDBC25638
:settings|412A6E9C039D477292765D0AA721A735B316CCC2A9C122B1E4DB3E8F80EF08B4
:storage|F1E3D820C3C244AF981265839EB9D074361638D6741867769AB9BA16D8691DCA
```

**Format:** `:project-path|SHA256-hash`

### Publishing a Single Module

Publish one module to local Maven:

```bash
./gradlew :storage:publishToArtifactSwapRepository
```

**What gets published:**
```
/Users/jason/.m2/repository/dev/jasonpearson/android/storage/F1E3D820.../
├── storage-F1E3D820...-sources.jar       # Source code
├── storage-F1E3D820...-sources.jar.md5   # Checksum
├── storage-F1E3D820...-sources.jar.sha1  # Checksum
├── storage-F1E3D820...-sources.jar.sha256 # Checksum
├── storage-F1E3D820...-sources.jar.sha512 # Checksum
├── storage-F1E3D820....aar                # Android library
├── storage-F1E3D820....aar.md5           # Checksum
├── storage-F1E3D820....aar.sha1          # Checksum
├── storage-F1E3D820....aar.sha256        # Checksum
├── storage-F1E3D820....aar.sha512        # Checksum
├── storage-F1E3D820....module            # Gradle module metadata
├── storage-F1E3D820....module.md5        # Checksum
├── storage-F1E3D820....module.sha1       # Checksum
├── storage-F1E3D820....module.sha256     # Checksum
├── storage-F1E3D820....module.sha512     # Checksum
├── storage-F1E3D820....pom               # Maven POM
├── storage-F1E3D820....pom.md5           # Checksum
├── storage-F1E3D820....pom.sha1          # Checksum
├── storage-F1E3D820....pom.sha256        # Checksum
└── storage-F1E3D820....pom.sha512        # Checksum
```

### Publishing All Library Modules

To publish all library modules at once:

```bash
# Publish each module individually
for project in analytics design:assets design:system experimentation home login mediaplayer onboarding settings storage; do
  echo "Publishing :$project"
  ./gradlew :$project:publishToArtifactSwapRepository
done
```

**Note:** The `:app` module is an Android application, not a library, so it's not published.

### Published Artifacts Structure

All artifacts are published under the configured Maven group:

```
/Users/jason/.m2/repository/dev/jasonpearson/android/
├── analytics/75FA01205D25C402F5AE5469CD62AC44832B7658F4503AAA36D9A7CF33C8170D/
├── design_assets/7A0C12E42CC26403AE99E6C7402390CEFDD51810909A112089F2A07B3B8F9DDE/
├── design_system/77BB164B9B7B081F66CD6C5AA50CCF430EB7A214762F1EA78616171C3CE57B98/
├── experimentation/390F9E43033610EA9450AEDDA768C6A25EA61BEEF6DB037F140517FFCDD6E550/
├── home/13B5E5A97B5D79783B1E9EF0E0B7BFEC7CE2D208317B338B68ABB1834D3A3D0D/
├── login/20B3A92F642B22C7FE57B7DA5E6ADDD9CB5B4C3BCCA512399DC04A28F6F60DE9/
├── mediaplayer/59BDC717B4A53AB8754B69716FE61D6586FF471CD5F76A7AEEB77C1768F3BD58/
├── onboarding/C63FAD4B7411F79B4A1FC88FE9D9041DDCA85E5BCCEBB5EC0D294C8DDBC25638/
├── settings/412A6E9C039D477292765D0AA721A735B316CCC2A9C122B1E4DB3E8F80EF08B4/
└── storage/F1E3D820C3C244AF981265839EB9D074361638D6741867769AB9BA16D8691DCA/
```

**Maven coordinates format:**
- Group: `dev.jasonpearson.android`
- Artifact: `storage` (project name with colons replaced by underscores)
- Version: `F1E3D820C3C244AF981265839EB9D074361638D6741867769AB9BA16D8691DCA` (SHA256 hash)

---

## Creating the BOM

### What is a BOM?

A **Bill of Materials (BOM)** is a Maven POM file that lists all available artifact versions. The artifact-swap plugin reads this BOM to know which artifacts are available for swapping.

**Example BOM content:**

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0">
  <groupId>dev.jasonpearson.android</groupId>
  <artifactId>bom</artifactId>
  <version>c8a2529cc49d66bb736a891410fd019e10869009</version>
  <name>bom</name>
  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>dev.jasonpearson.android</groupId>
        <artifactId>analytics</artifactId>
        <version>75FA01205D25C402F5AE5469CD62AC44832B7658F4503AAA36D9A7CF33C8170D</version>
      </dependency>
      <dependency>
        <groupId>dev.jasonpearson.android</groupId>
        <artifactId>storage</artifactId>
        <version>F1E3D820C3C244AF981265839EB9D074361638D6741867769AB9BA16D8691DCA</version>
      </dependency>
      <!-- ... more dependencies ... -->
    </dependencies>
  </dependencyManagement>
  <packaging>pom</packaging>
  <modelVersion>4.0.0</modelVersion>
</project>
```

### BOM Version Strategy

The BOM version is a **git commit SHA**. The artifact-swap plugin searches git history for BOMs that match commit SHAs.

**Critical requirement:** The BOM version must be a "shared commit" - a commit that exists both locally and on the remote branch.

### Publishing a BOM

Use the artifact-swap CLI tool to publish a BOM:

```bash
# Get the current git commit
COMMIT_SHA=$(git rev-parse HEAD)

# Publish BOM for current commit
/Users/jason/github/artifact-swap/artifactswap-shadow-0.1.5-SNAPSHOT/bin/artifactswap \
  bom-publisher \
  --local \
  --bom-version=$COMMIT_SHA \
  --hash-file-location=/Users/jason/kaeawc/android-build/artifact-sync-hash-output
```

**Command breakdown:**
- `bom-publisher` - Subcommand to publish a BOM
- `--local` - Publish to local Maven repository (instead of Artifactory)
- `--bom-version` - Git commit SHA to use as BOM version
- `--hash-file-location` - Path to the file containing module hashes

**Expected output:**

```
Starting BOM publisher
Reading hash output from /Users/jason/kaeawc/android-build/artifact-sync-hash-output
Collecting available artifacts from repository
Project :app not found in local Maven repository
Got 10 dependencies for this BOM
Publishing BOM artifact
BOM written to local Maven repository: /Users/jason/.m2/repository/dev/jasonpearson/android/bom/c8a2529.../bom-c8a2529....pom
BOM artifact published!
Publishing BOM metadata
BOM publisher completed with result: SUCCESS_BOM_AND_METADATA_PUBLISHED
```

**Note:** You'll see errors about `analytics.example.com` - these are harmless. The tool tries to send telemetry but fails because it's a dummy URL.

### Publishing BOM for Merge-Base Commit

To guarantee the BOM can be found, publish it for the merge-base commit:

```bash
# Find merge-base commit (guaranteed to be shared)
MERGE_BASE=$(git merge-base HEAD origin/fix-build-script-order)

# Publish BOM for merge-base
/Users/jason/github/artifact-swap/artifactswap-shadow-0.1.5-SNAPSHOT/bin/artifactswap \
  bom-publisher \
  --local \
  --bom-version=$MERGE_BASE \
  --hash-file-location=/Users/jason/kaeawc/android-build/artifact-sync-hash-output
```

**Why this works:**
- The merge-base is the most recent commit that exists in both your local branch and the remote branch
- The artifact-swap plugin searches for BOMs starting from this commit
- Publishing a BOM for this commit ensures it will be found

### BOM Storage Location

BOMs are stored in local Maven repository:

```
/Users/jason/.m2/repository/dev/jasonpearson/android/bom/
├── c8a2529cc49d66bb736a891410fd019e10869009/
│   └── bom-c8a2529cc49d66bb736a891410fd019e10869009.pom
├── db11f49fed42cc6e15cafecfa46c209c0a44c9a1/
│   └── bom-db11f49fed42cc6e15cafecfa46c209c0a44c9a1.pom
└── 2eb4cec7269e37e98dd2ae80fda6f35fd1d20a2a/
    └── bom-2eb4cec7269e37e98dd2ae80fda6f35fd1d20a2a.pom
```

### Initial Setup: Publishing BOMs for All Recent Commits

For initial setup, it's helpful to publish BOMs for several recent commits:

```bash
# Get recent commit SHAs
git log --oneline -5 | awk '{print $1}'

# Publish BOM for each commit
for commit in $(git log --oneline -5 --format=%H); do
  echo "Publishing BOM for commit $commit"
  /Users/jason/github/artifact-swap/artifactswap-shadow-0.1.5-SNAPSHOT/bin/artifactswap \
    bom-publisher \
    --local \
    --bom-version=$commit \
    --hash-file-location=/Users/jason/kaeawc/android-build/artifact-sync-hash-output
done
```

---

## Verification

### Testing Artifact Swap Activation

Artifact swap only activates during **IDE sync**, not command-line builds. To simulate IDE sync from command line:

```bash
# Test with IDE sync flag
./gradlew help -Didea.sync.active=true
```

**Expected output (artifact swap active):**
```
Using Artifact Swap! See https://go/artifact-sync for docs.
You can disable this by setting artifactswap.enabled=false in your gradle properties
```

**Expected output (artifact swap inactive - normal command-line build):**
```
Spotlight included 11 projects
```

### Verifying Published Artifacts

Check that all modules are published:

```bash
# List published modules
ls -1 /Users/jason/.m2/repository/dev/jasonpearson/android/

# Expected output:
# analytics
# bom
# design_assets
# design_system
# experimentation
# home
# login
# mediaplayer
# onboarding
# settings
# storage

# Count AAR files (should be 10 - one per library module)
find /Users/jason/.m2/repository/dev/jasonpearson/android/ -name "*.aar" | wc -l
```

### Verifying BOM Versions

Check published BOM versions:

```bash
# List BOM versions
ls -1 /Users/jason/.m2/repository/dev/jasonpearson/android/bom/

# Each directory name is a git commit SHA
```

### Testing IDE Sync

1. **Open project in IntelliJ IDEA or Android Studio**

2. **Trigger Gradle sync** (File → Sync Project with Gradle Files)

3. **Check Build Output for confirmation:**
   ```
   Using Artifact Swap! See https://go/artifact-sync for docs.
   ```

4. **Verify faster sync time** - should be noticeably faster than before

### Troubleshooting Failed IDE Sync

If IDE sync fails with BOM error:

1. **Check current commit:**
   ```bash
   git rev-parse HEAD
   ```

2. **Verify BOM exists for that commit:**
   ```bash
   ls /Users/jason/.m2/repository/dev/jasonpearson/android/bom/$(git rev-parse HEAD)/
   ```

3. **If BOM missing, publish it:**
   ```bash
   /Users/jason/github/artifact-swap/artifactswap-shadow-0.1.5-SNAPSHOT/bin/artifactswap \
     bom-publisher \
     --local \
     --bom-version=$(git rev-parse HEAD) \
     --hash-file-location=/Users/jason/kaeawc/android-build/artifact-sync-hash-output
   ```

4. **Clean and retry:**
   ```bash
   rm -rf .gradle .idea
   # Reopen project in IDE and sync
   ```

---

## Daily Workflow

### Making Code Changes

When you modify code and want to use artifact swap with the changes:

#### Step 1: Make Changes and Commit

```bash
# Make your code changes
# ... edit files ...

# Commit changes
git add .
git commit -m "Your commit message"
```

#### Step 2: Publish Changed Modules

```bash
# Publish the module you changed
./gradlew :your-changed-module:publishToArtifactSwapRepository

# Example: if you changed the storage module
./gradlew :storage:publishToArtifactSwapRepository
```

**Note:** Only publish modules that you actually changed. Unchanged modules already have their artifacts published.

#### Step 3: Publish New BOM

```bash
# Publish BOM for your new commit
COMMIT_SHA=$(git rev-parse HEAD)
/Users/jason/github/artifact-swap/artifactswap-shadow-0.1.5-SNAPSHOT/bin/artifactswap \
  bom-publisher \
  --local \
  --bom-version=$COMMIT_SHA \
  --hash-file-location=/Users/jason/kaeawc/android-build/artifact-sync-hash-output
```

#### Step 4: Push to Origin (Recommended)

```bash
# Push your commit to make it "shared"
git push origin fix-build-script-order
```

This ensures the BOM can be found by the artifact-swap plugin.

#### Step 5: Sync in IDE

Trigger Gradle sync in your IDE. Artifact swap will use your newly published artifacts.

### Switching Branches

When switching to a different branch:

1. **Switch branch:**
   ```bash
   git checkout other-branch
   ```

2. **Check if BOM exists:**
   ```bash
   COMMIT_SHA=$(git rev-parse HEAD)
   ls /Users/jason/.m2/repository/dev/jasonpearson/android/bom/$COMMIT_SHA/ 2>/dev/null || echo "BOM not found"
   ```

3. **If BOM missing, publish it:**
   ```bash
   /Users/jason/github/artifact-swap/artifactswap-shadow-0.1.5-SNAPSHOT/bin/artifactswap \
     bom-publisher \
     --local \
     --bom-version=$COMMIT_SHA \
     --hash-file-location=/Users/jason/kaeawc/android-build/artifact-sync-hash-output
   ```

4. **Sync in IDE**

### Automation Script

Create a helper script to automate the workflow:

```bash
#!/bin/bash
# File: publish-for-artifact-swap.sh

set -e

COMMIT_SHA=$(git rev-parse HEAD)
REPO_ROOT="/Users/jason/kaeawc/android-build"
CLI_PATH="/Users/jason/github/artifact-swap/artifactswap-shadow-0.1.5-SNAPSHOT/bin/artifactswap"

echo "Publishing BOM for commit: $COMMIT_SHA"

"$CLI_PATH" bom-publisher \
  --local \
  --bom-version="$COMMIT_SHA" \
  --hash-file-location="$REPO_ROOT/artifact-sync-hash-output" \
  2>&1 | grep -v "analytics.example.com\|eventstream\|UnknownHostException"

echo "✅ BOM published successfully!"
echo "You can now sync in your IDE."
```

Make it executable:
```bash
chmod +x publish-for-artifact-swap.sh
```

Use it:
```bash
./publish-for-artifact-swap.sh
```

---

## Troubleshooting

### Issue: "Failed to determine BOM version"

**Symptoms:**
```
xyz.block.artifactswap.core.module_selector.RealArtifactSwapModuleSelector$ModuleSelectorException$FailedDeterminingBomVersionException: 
Failed to determine BOM version
Caused by: java.lang.IllegalStateException: 
Traversed 5000 commits from origin/fix-build-script-order and found no matching BOMs
```

**Diagnosis:**

1. Check if you have unpushed commits:
   ```bash
   git log origin/fix-build-script-order..HEAD --oneline
   ```

2. Find merge-base commit:
   ```bash
   git merge-base HEAD origin/fix-build-script-order
   ```

3. Check if BOM exists for merge-base:
   ```bash
   MERGE_BASE=$(git merge-base HEAD origin/fix-build-script-order)
   ls /Users/jason/.m2/repository/dev/jasonpearson/android/bom/$MERGE_BASE/
   ```

**Solutions:**

**Option 1: Publish BOM for merge-base** (Quick fix)
```bash
MERGE_BASE=$(git merge-base HEAD origin/fix-build-script-order)
/Users/jason/github/artifact-swap/artifactswap-shadow-0.1.5-SNAPSHOT/bin/artifactswap \
  bom-publisher \
  --local \
  --bom-version=$MERGE_BASE \
  --hash-file-location=/Users/jason/kaeawc/android-build/artifact-sync-hash-output
```

**Option 2: Push commits to origin** (Better long-term)
```bash
git push origin fix-build-script-order
```
Then publish BOM for current HEAD:
```bash
/Users/jason/github/artifact-swap/artifactswap-shadow-0.1.5-SNAPSHOT/bin/artifactswap \
  bom-publisher \
  --local \
  --bom-version=$(git rev-parse HEAD) \
  --hash-file-location=/Users/jason/kaeawc/android-build/artifact-sync-hash-output
```

### Issue: Publishing tasks not found

**Symptoms:**
```
Cannot locate tasks that match ':analytics:publishToArtifactSwapRepository' 
as task 'publishToArtifactSwapRepository' not found in project ':analytics'.
```

**Diagnosis:**

Check gradle.properties:
```bash
grep "artifactswap\." gradle.properties
```

**Solution:**

Ensure both properties are `true`:
```properties
artifactswap.enabled=true
artifactswap.publishingEnabled=true
```

If you disabled `artifactswap.enabled` to work around the BOM issue, re-enable it and fix the BOM issue instead.

### Issue: Artifact swap not activating in IDE

**Symptoms:**
- IDE sync completes but you don't see "Using Artifact Swap!" message
- Sync time hasn't improved

**Diagnosis:**

1. Check if artifact swap is enabled:
   ```bash
   grep "artifactswap.enabled" gradle.properties
   ```

2. Test from command line:
   ```bash
   ./gradlew help -Didea.sync.active=true 2>&1 | grep "Artifact Swap"
   ```

**Solutions:**

1. **Ensure configuration is correct:**
   ```properties
   artifactswap.enabled=true
   artifactswap.publishingEnabled=true
   ```

2. **Clean IDE cache:**
   ```bash
   rm -rf .gradle .idea
   ```

3. **Restart IDE** and re-sync

4. **Check IDE is using correct JDK** (should match project JDK)

5. **Verify BOM exists:**
   ```bash
   COMMIT_SHA=$(git rev-parse HEAD)
   ls /Users/jason/.m2/repository/dev/jasonpearson/android/bom/$COMMIT_SHA/
   ```

### Issue: Module artifacts not found

**Symptoms:**
```
Project :storage not found in local Maven repository
```

**Diagnosis:**

Check if module is published:
```bash
# Get module version from hash file
grep ":storage" artifact-sync-hash-output

# Check if artifact exists
STORAGE_VERSION=$(grep ":storage" artifact-sync-hash-output | cut -d'|' -f2)
ls /Users/jason/.m2/repository/dev/jasonpearson/android/storage/$STORAGE_VERSION/
```

**Solution:**

Publish the missing module:
```bash
./gradlew :storage:publishToArtifactSwapRepository
```

Then re-publish the BOM:
```bash
/Users/jason/github/artifact-swap/artifactswap-shadow-0.1.5-SNAPSHOT/bin/artifactswap \
  bom-publisher \
  --local \
  --bom-version=$(git rev-parse HEAD) \
  --hash-file-location=/Users/jason/kaeawc/android-build/artifact-sync-hash-output
```

### Issue: Analytics errors in BOM publisher

**Symptoms:**
```
An exception occurred while sending Eventstream events.
java.net.UnknownHostException: analytics.example.com
```

**Diagnosis:**

This is expected behavior. The BOM publisher tries to send telemetry to the analytics endpoint configured in `gradle.properties`:
```properties
artifactswap.artifactoryBaseUrl=https://artifactory.example.com
```

**Solution:**

**This is not an error!** The BOM is still published successfully. You'll see this message at the end:
```
BOM publisher completed with result: SUCCESS_BOM_AND_METADATA_PUBLISHED
```

To avoid these messages, you can:
1. Ignore them (recommended - they don't affect functionality)
2. Filter them out in your script:
   ```bash
   /Users/jason/github/artifact-swap/artifactswap-shadow-0.1.5-SNAPSHOT/bin/artifactswap \
     bom-publisher ... 2>&1 | grep -v "analytics.example.com"
   ```

### Issue: Configuration cache warnings

**Symptoms:**
```
29 problems were found storing the configuration cache, 6 of which seem unique.
- Build file 'app/build.gradle.kts': Project ':app' cannot dynamically look up a property...
```

**Diagnosis:**

These are configuration cache compatibility warnings, not errors. They come from:
1. The artifact-swap plugin itself (known issues)
2. Other plugins (e.g., module-graph-assertion, shadow plugin)

**Solution:**

**These warnings do not affect artifact swap functionality.** You can:
1. Ignore them (recommended)
2. The build still succeeds: `BUILD SUCCESSFUL`
3. Configuration cache is still used despite the warnings

These warnings will be fixed in future versions of the artifact-swap plugin.

### Issue: Slow sync even with artifact swap

**Symptoms:**
- Artifact swap is active
- BOM found
- But sync time hasn't improved significantly

**Possible Causes:**

1. **All projects are being included** - Check Spotlight configuration
2. **Working on too many modules** - Artifact swap only swaps modules you're NOT working on
3. **Configuration cache not being used**
4. **First sync after changes** - Initial sync is always slower

**Solutions:**

1. **Check which projects are included:**
   - Look for "Spotlight included X projects" message
   - If X is close to total project count, Spotlight isn't limiting the projects

2. **Use Spotlight to select specific modules:**
   - Install Spotlight IDE plugin
   - Configure it to only include modules you're actively working on

3. **Verify configuration cache is enabled:**
   ```bash
   grep "configuration-cache" gradle.properties
   ```
   Should be:
   ```properties
   org.gradle.configuration-cache=true
   ```

4. **Compare sync times:**
   - Disable artifact swap: `artifactswap.enabled=false`
   - Sync and time it
   - Enable artifact swap: `artifactswap.enabled=true`
   - Sync and time it
   - Compare the difference

---

## Advanced Configuration

### Using Artifactory Instead of Local Maven

To publish to a remote Artifactory instance instead of local Maven:

1. **Update gradle.properties:**
   ```properties
   artifactswap.artifactRepo.url=https://your-artifactory.com/artifactory/repo-name
   artifactswap.artifactRepo.username=your-username
   artifactswap.artifactRepo.password=your-token
   ```

2. **Publish modules** (same command):
   ```bash
   ./gradlew :your-module:publishToArtifactSwapRepository
   ```

3. **Publish BOM** (remove `--local` flag):
   ```bash
   /Users/jason/github/artifact-swap/artifactswap-shadow-0.1.5-SNAPSHOT/bin/artifactswap \
     bom-publisher \
     --bom-version=$(git rev-parse HEAD) \
     --hash-file-location=/Users/jason/kaeawc/android-build/artifact-sync-hash-output
   ```

### Using Token File for Authentication

For CI/CD, use a token file:

1. **Create token file:**
   ```bash
   mkdir -p ~/.secrets
   echo "your-artifactory-token" > ~/.secrets/artifactory-token.txt
   ```

2. **Set environment variable:**
   ```bash
   export SECRETS_PATH="$HOME/.secrets"
   ```

3. **Update gradle.properties:**
   ```properties
   artifactswap.artifactoryPublisherTokenFileName=artifactory-token.txt
   ```

### Configuring for CI/CD

Example CI/CD workflow:

```yaml
# .github/workflows/publish-artifacts.yml
name: Publish Artifacts

on:
  push:
    branches: [main, develop]

jobs:
  publish:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
        with:
          fetch-depth: 0  # Need full history for artifact-swap
      
      - name: Set up JDK
        uses: actions/setup-java@v3
        with:
          java-version: '21'
          distribution: 'temurin'
      
      - name: Publish changed modules
        run: |
          # Detect changed modules and publish them
          ./gradlew publishAllPublicationsToArtifactSwapRepository
      
      - name: Publish BOM
        run: |
          COMMIT_SHA=$(git rev-parse HEAD)
          /path/to/artifactswap bom-publisher \
            --bom-version=$COMMIT_SHA \
            --hash-file-location=artifact-sync-hash-output \
            --build-id=${{ github.run_id }} \
            --ci-type=github-actions
```

---

## Summary

### What We Set Up

1. ✅ **Configured artifact-swap plugin** in `settings.gradle.kts`
2. ✅ **Fixed plugin conflicts** by removing duplicate plugin declarations
3. ✅ **Enabled publishing** in `gradle.properties`
4. ✅ **Published all 10 library modules** to local Maven
5. ✅ **Created BOMs** for multiple git commits
6. ✅ **Verified artifact swap activation** during IDE sync

### Key Files Modified

- `settings.gradle.kts` - Plugin configuration and buildscript classpath
- `gradle.properties` - Feature flags and artifact swap configuration
- `build.gradle.kts` - Removed duplicate plugin declarations
- `artifact-sync-hash-output` - Module content hashes (already present)

### Published Artifacts

**Modules in local Maven:**
- analytics, design:assets, design:system, experimentation, home, login, mediaplayer, onboarding, settings, storage

**BOMs published:**
- `c8a2529cc49d66bb736a891410fd019e10869009` (merge-base commit)
- `db11f49fed42cc6e15cafecfa46c209c0a44c9a1` (older commit)
- `2eb4cec7269e37e98dd2ae80fda6f35fd1d20a2a` (current HEAD)

### Performance Impact

**Before artifact swap:**
- 11 projects configured every sync
- Full dependency resolution for all modules
- Slower IDE sync times

**After artifact swap:**
- Only active projects configured
- Inactive projects loaded as pre-built artifacts
- Significantly faster IDE sync times

### Daily Usage

**When making changes:**
```bash
# 1. Make changes and commit
git add . && git commit -m "Your changes"

# 2. Publish changed module
./gradlew :your-module:publishToArtifactSwapRepository

# 3. Publish BOM
./publish-for-artifact-swap.sh  # Or use the manual command

# 4. Sync in IDE
```

**Expected behavior:**
- IDE sync should show "Using Artifact Swap!"
- Sync time should be noticeably faster
- All builds and tests should work normally

---

## References

- [Artifact Swap Repository](../../github/artifact-swap)
- [Artifact Swap README](../../github/artifact-swap/README.md)
- Module hash file: `artifact-sync-hash-output`
- CLI tool: `/Users/jason/github/artifact-swap/artifactswap-shadow-0.1.5-SNAPSHOT/bin/artifactswap`
- Local Maven: `/Users/jason/.m2/repository/dev/jasonpearson/android/`

---

## Document History

- **2024-12-22**: Initial setup documentation
- **Repository**: kaeawc/android-build
- **Branch**: fix-build-script-order
- **Artifact Group**: dev.jasonpearson.android
