# Artifact Swap Logging Guide

This guide explains what log output to expect when using the Artifact Swap Gradle plugin and how to determine if it's working correctly.

## When Artifact Swap is Active and Working

You should see these key log messages during Gradle sync:

### 1. Activation Messages (at the start)

```
Using Artifact Swap! See https://go/artifact-sync for docs.
You can disable this by setting ARTIFACT_SWAP_ENABLED=false in your gradle properties
```

### 2. Module Selection Metrics (CRITICAL)

```
Artifact Swap module selection: X selected out of Y candidates (explicit: A, always-keep: B, local changes: C, missing artifact: D, excluded: E)
```

**This line is the key to knowing if swapping is working!** Here's how to interpret it:

- **`Y candidates`**: Total number of projects in your repo
- **`X selected`**: Projects being built from source (NOT swapped)
- **`excluded: E`**: **Projects swapped with artifacts (THIS SHOULD BE HIGH!)**

#### Interpreting the Metrics

**Breakdown of the selection reasons:**
- **`explicit`**: Projects you explicitly requested to work on
- **`always-keep`**: Projects configured to always build from source
- **`local changes`**: Projects with uncommitted changes detected by git
- **`missing artifact`**: Projects that need to be built because their artifact isn't available
- **`excluded`**: Projects successfully replaced with artifacts 🎯

#### ✅ Perfect Scenario

If you're working on only a few modules, you want:
- **Low `X selected`** (only the projects you're actively working on)
- **High `excluded`** (most projects replaced with artifacts)
