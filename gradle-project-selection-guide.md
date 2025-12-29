# How to Select Projects for Artifact Swap Using Gradle Commands

**Date:** December 26, 2025  
**Repository:** `android-build`  
**Branch:** `fix-build-script-order`

---

## Overview

When using artifact-swap, you need to tell it which projects you want to work on. Artifact-swap will then:
- **Build those projects from source** (the ones you selected)
- **Swap all other projects with pre-built artifacts** (for faster sync)

This guide covers the available methods to select projects.

---

## Method 1: Edit `gradle/ide-projects.txt` (Recommended) ✅

### How It Works

The **simplest and most common** way to select projects is by editing the `gradle/ide-projects.txt` file in your repository.

### Quick Commands

**Select a single module to work on:**
```bash
# Work on login module
echo -e ":app\n:login" > gradle/ide-projects.txt
```

**Select multiple modules:**
```bash
# Work on login and storage
echo -e ":app\n:login\n:storage" > gradle/ide-projects.txt
```

**Work on the entire design system:**
```bash
# Work on design modules
echo -e ":app\n:design:assets\n:design:system" > gradle/ide-projects.txt
```

**Work on everything (disable artifact swap benefits):**
```bash
# Copy all projects to ide-projects.txt
cp gradle/all-projects.txt gradle/ide-projects.txt
```

**Clear selection (work on only what has local changes):**
```bash
# Empty the file - only projects with uncommitted changes will be built
echo -n "" > gradle/ide-projects.txt
# Or just put :app
echo ":app" > gradle/ide-projects.txt
```

### Manual Editing

You can also edit `gradle/ide-projects.txt` directly:

```bash
# Open in your editor
vim gradle/ide-projects.txt
# or
code gradle/ide-projects.txt
```

**File format:**
```
:app
:login
:storage
```

**Important:**
- One project per line
- Use Gradle project paths (`:module` or `:parent:child`)
- Must start with `:` (colon)
- No trailing commas or semicolons

### Verification

After editing, verify the selection worked:

```bash
./gradlew help -Didea.sync.active=true 2>&1 | grep "module selection"
```

**Expected output:**
```
Artifact Swap module selection: 3 selected out of 10 candidates 
(explicit: 2, local changes: 1, excluded: 7)
```

Look for:
- **`explicit: X`** - Should match the count of projects in your `ide-projects.txt`
- **`excluded: Y`** - Should be high (these are swapped with artifacts)

---

## Method 2: Using Gradle Properties (Command Line)

### Override via Command Line

You can override the `ide-projects.txt` file using gradle properties:

```bash
# Try overriding with a property (experimental - may not work in all versions)
./gradlew help -Psquare.spotlight.projects=":app,:login" -Didea.sync.active=true
```

**Note:** Based on testing, this property **does not override** the `ide-projects.txt` file in the current version of artifact-swap. The bundled Spotlight functionality reads from the file system, not from properties.

### Set in `gradle.properties`

You could try adding a property to `gradle.properties`, though this hasn't been verified:

```properties
# Unverified - may not work
square.spotlight.projects=:app,:login,:storage
```

**Recommendation:** Stick with editing `gradle/ide-projects.txt` as it's the proven method.

---

## Method 3: Helper Scripts for Common Scenarios

### Create Convenience Scripts

You can create shell scripts for common workflows:

**`select-login.sh`:**
```bash
#!/bin/bash
# Select login module for development
echo -e ":app\n:login" > gradle/ide-projects.txt
echo "✅ Selected :app and :login for development"
echo "Run Gradle sync in your IDE now"
```

**`select-storage.sh`:**
```bash
#!/bin/bash
# Select storage module for development
echo -e ":app\n:storage" > gradle/ide-projects.txt
echo "✅ Selected :app and :storage for development"
echo "Run Gradle sync in your IDE now"
```

**`select-design.sh`:**
```bash
#!/bin/bash
# Select all design modules
echo -e ":app\n:design:assets\n:design:system" > gradle/ide-projects.txt
echo "✅ Selected :app and design modules for development"
echo "Run Gradle sync in your IDE now"
```

**`select-all.sh`:**
```bash
#!/bin/bash
# Work on everything (disables artifact swap benefits)
cp gradle/all-projects.txt gradle/ide-projects.txt
echo "⚠️  Selected ALL projects - artifact swap will not swap anything"
echo "Run Gradle sync in your IDE now"
```

**Make them executable:**
```bash
chmod +x select-*.sh
```

**Usage:**
```bash
./select-login.sh
# Then sync in IDE
```

---

## Method 4: Interactive Project Selector Script

### Advanced: Dynamic Selection Script

Create a script that lets you interactively choose modules:

**`select-projects.sh`:**
```bash
#!/bin/bash
# Interactive project selector for artifact-swap

PROJECTS_FILE="gradle/ide-projects.txt"
ALL_PROJECTS_FILE="gradle/all-projects.txt"

echo "Available projects:"
echo "=================="
cat "$ALL_PROJECTS_FILE" | nl
echo ""
echo "Current selection in $PROJECTS_FILE:"
if [ -s "$PROJECTS_FILE" ]; then
    cat "$PROJECTS_FILE"
else
    echo "(empty)"
fi
echo ""
echo "Enter project numbers to work on (space-separated, e.g., '1 3 5'):"
echo "Or press Enter to edit $PROJECTS_FILE manually"
read -r selection

if [ -z "$selection" ]; then
    ${EDITOR:-vim} "$PROJECTS_FILE"
else
    # Always include :app
    echo ":app" > "$PROJECTS_FILE"
    
    # Add selected projects
    for num in $selection; do
        project=$(sed "${num}q;d" "$ALL_PROJECTS_FILE")
        if [ "$project" != ":app" ] && [ -n "$project" ]; then
            echo "$project" >> "$PROJECTS_FILE"
        fi
    done
    
    echo ""
    echo "✅ Updated $PROJECTS_FILE:"
    cat "$PROJECTS_FILE"
    echo ""
    echo "Run Gradle sync in your IDE now"
fi
```

**Make it executable:**
```bash
chmod +x select-projects.sh
```

**Usage:**
```bash
./select-projects.sh
# Choose projects by number
# Then sync in IDE
```

---

## Method 5: Git-Aware Selection (Smart)

### Automatically Select Based on Changed Files

Create a script that automatically selects projects based on which files you've modified:

**`select-changed.sh`:**
```bash
#!/bin/bash
# Select projects based on git changes

PROJECTS_FILE="gradle/ide-projects.txt"

# Start with :app
echo ":app" > "$PROJECTS_FILE"

# Find all modified directories
changed_dirs=$(git status --short | awk '{print $2}' | cut -d'/' -f1 | sort -u)

# Map directories to project paths
for dir in $changed_dirs; do
    case "$dir" in
        "analytics") echo ":analytics" >> "$PROJECTS_FILE" ;;
        "storage") echo ":storage" >> "$PROJECTS_FILE" ;;
        "login") echo ":login" >> "$PROJECTS_FILE" ;;
        "home") echo ":home" >> "$PROJECTS_FILE" ;;
        "settings") echo ":settings" >> "$PROJECTS_FILE" ;;
        "onboarding") echo ":onboarding" >> "$PROJECTS_FILE" ;;
        "mediaplayer") echo ":mediaplayer" >> "$PROJECTS_FILE" ;;
        "experimentation") echo ":experimentation" >> "$PROJECTS_FILE" ;;
        "design")
            # Check if assets or system subdirectories were changed
            if git status --short | grep -q "design/assets"; then
                echo ":design:assets" >> "$PROJECTS_FILE"
            fi
            if git status --short | grep -q "design/system"; then
                echo ":design:system" >> "$PROJECTS_FILE"
            fi
            ;;
    esac
done

# Remove duplicates and sort
sort -u "$PROJECTS_FILE" -o "$PROJECTS_FILE"

echo "✅ Auto-selected projects based on git changes:"
cat "$PROJECTS_FILE"
echo ""
echo "Note: Artifact-swap already detects local changes automatically,"
echo "but this script updates ide-projects.txt to match your workflow"
```

**Note:** Artifact-swap **already automatically includes projects with uncommitted changes**, so this script is optional. It's useful if you want your `ide-projects.txt` to reflect what you're actually working on.

---

## Understanding Automatic Local Change Detection

### How It Works

Artifact-swap has **built-in git integration** that automatically detects projects with uncommitted changes:

```
Artifact Swap module selection: 3 selected out of 10 candidates 
(explicit: 2, local changes: 1, missing artifact: 0, excluded: 7)
```

In this example:
- **explicit: 2** - Projects in `ide-projects.txt` (`:app`, `:login`)
- **local changes: 1** - Project with uncommitted changes (`:onboarding`)
- **Total selected: 3** - All three are built from source

### What Counts as a Local Change?

- Modified files in the project directory
- New (untracked) files in the project directory
- Deleted files in the project directory

### Example

```bash
$ git status --short
M  onboarding/build.gradle.kts
?? new-feature.md
```

Even if `:onboarding` is **not** in `ide-projects.txt`, it will be built from source because of the modified `build.gradle.kts` file.

---

## Best Practices

### 1. Always Include `:app`

The `:app` module is your application entry point and usually needs to be built to run/test:

```bash
# Good - includes :app
echo -e ":app\n:login" > gradle/ide-projects.txt

# Bad - missing :app (unless you're only building libraries)
echo ":login" > gradle/ide-projects.txt
```

### 2. Select Only What You Need

The **fewer projects you select**, the **faster your sync**:

```bash
# Working on one feature - FAST
echo -e ":app\n:login" > gradle/ide-projects.txt
# Result: 2 projects built, 8 swapped

# Working on multiple features - SLOWER
echo -e ":app\n:login\n:storage\n:analytics\n:home" > gradle/ide-projects.txt
# Result: 5 projects built, 5 swapped

# Working on everything - SLOWEST (no benefit)
cp gradle/all-projects.txt gradle/ide-projects.txt
# Result: 11 projects built, 0 swapped
```

### 3. Keep `:app` Separate from Libraries

When working on a library module, you typically don't need to edit `:app`:

```bash
# Developing login feature
echo -e ":app\n:login" > gradle/ide-projects.txt
```

### 4. Use Version Control for `ide-projects.txt`

**Option A: Ignore it** (each developer manages their own):
```bash
# Add to .gitignore
echo "gradle/ide-projects.txt" >> .gitignore
```

**Option B: Commit a default** (team default, developers override locally):
```bash
# Commit a sensible default (just :app)
echo ":app" > gradle/ide-projects.txt
git add gradle/ide-projects.txt
git commit -m "Set default ide-projects.txt"

# Each developer can override locally without committing
```

**Current status:** `gradle/ide-projects.txt` is already in `.gitignore` (line 42), so it's developer-specific.

### 5. Create Aliases

Add to your shell profile (`~/.bashrc`, `~/.zshrc`, etc.):

```bash
# Gradle project selection aliases
alias gradle-select-login='echo -e ":app\n:login" > gradle/ide-projects.txt && echo "Selected :login"'
alias gradle-select-storage='echo -e ":app\n:storage" > gradle/ide-projects.txt && echo "Selected :storage"'
alias gradle-select-all='cp gradle/all-projects.txt gradle/ide-projects.txt && echo "Selected ALL (slow)"'
alias gradle-show-selection='cat gradle/ide-projects.txt'
```

**Usage:**
```bash
gradle-select-login
# Then sync in IDE
```

---

## Troubleshooting

### Projects Not Being Excluded

**Check your selection:**
```bash
cat gradle/ide-projects.txt
```

**Verify artifact-swap sees it:**
```bash
./gradlew help -Didea.sync.active=true 2>&1 | grep "module selection"
```

**Expected:** `excluded` count should be high

### All Projects Still Selected

**Possible causes:**
1. **Empty file** - Populate it with at least `:app`
2. **Wrong format** - Must use `:module` format, not `module/`
3. **Wrong path** - Check project names in `gradle/all-projects.txt`

**Fix:**
```bash
# Copy example from all-projects.txt
head -2 gradle/all-projects.txt > gradle/ide-projects.txt
cat gradle/ide-projects.txt
```

### Projects Being Built Despite Not Being Selected

This is **expected behavior** if those projects have uncommitted changes:

```bash
# Check for local changes
git status

# To exclude them, commit or stash your changes
git add .
git commit -m "Save work"
# or
git stash
```

---

## Quick Reference

### List All Available Projects
```bash
cat gradle/all-projects.txt
```

### Show Current Selection
```bash
cat gradle/ide-projects.txt
```

### Change Selection (Quick)
```bash
# Login module
echo -e ":app\n:login" > gradle/ide-projects.txt

# Storage module
echo -e ":app\n:storage" > gradle/ide-projects.txt

# Multiple modules
echo -e ":app\n:login\n:storage\n:analytics" > gradle/ide-projects.txt
```

### Verify Selection
```bash
./gradlew help -Didea.sync.active=true 2>&1 | grep "module selection"
```

### Reset to Default
```bash
echo ":app" > gradle/ide-projects.txt
```

---

## Summary

### Primary Method
**Edit `gradle/ide-projects.txt`** - This is the standard, documented way to select projects.

### Quick Commands
```bash
# Select login
echo -e ":app\n:login" > gradle/ide-projects.txt

# Select storage  
echo -e ":app\n:storage" > gradle/ide-projects.txt

# Verify
./gradlew help -Didea.sync.active=true 2>&1 | grep "module selection"
```

### What to Expect
- **High `excluded` count** = Good performance
- **Low `selected` count** = Faster sync
- **Automatic local change detection** = Smart builds

---

## Additional Resources

- [artifact-swap-diagnosis.md](artifact-swap-diagnosis.md) - Diagnosis of why artifact swap wasn't working
- [ARTIFACT_SWAP_SETUP.md](ARTIFACT_SWAP_SETUP.md) - Complete setup guide
- [artifact-swap-logging-guide.md](artifact-swap-logging-guide.md) - How to interpret sync logs
- `gradle/ide-projects.txt` - Your project selection file
- `gradle/all-projects.txt` - List of all available projects
