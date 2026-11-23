#!/usr/bin/env bash

INSTALL_KTFMT_WHEN_MISSING=${INSTALL_KTFMT_WHEN_MISSING:-false}
ONLY_TOUCHED_FILES=${ONLY_TOUCHED_FILES:-true}
ONLY_CHANGED_SINCE_SHA=${ONLY_CHANGED_SINCE_SHA:-""}

# Files per ktfmt process: one JVM per file spent most of the check starting JVMs (~2.5 min
# for 240 files on CI). Batches run in parallel, one per core.
KTFMT_BATCH_SIZE=${KTFMT_BATCH_SIZE:-100}

# Check if ktfmt is installed
if ! command -v ktfmt &>/dev/null; then
    echo "ktfmt missing"
    if [[ "${INSTALL_KTFMT_WHEN_MISSING}" == "true" ]]; then
      ci/install_ktfmt.sh
      # Ensure ktfmt is in PATH for subsequent commands
      export PATH="$HOME/bin:$PATH"
    else
      if [[ "$OSTYPE" == "darwin"* ]]; then
        # macos specific advice
        echo "Try 'brew install ktfmt' or run with INSTALL_KTFMT_WHEN_MISSING=true"
      else
        echo "Consult your OS package manager"
      fi
      exit 1
    fi
fi

# Verify ktfmt is available
if ! command -v ktfmt &>/dev/null; then
    echo "Error: ktfmt is not available in PATH"
    exit 1
fi

# Start the timer
start_time=$(bash -c "$(pwd)/ci/get_timestamp.sh")

# run ktfmt as efficiently as possible against all kt and kts source files
if [[ -n "$ONLY_CHANGED_SINCE_SHA" ]]; then
  # Validate that the SHA exists in git history
  if ! git rev-parse --verify "$ONLY_CHANGED_SINCE_SHA" &>/dev/null; then
    echo "Error: Invalid git SHA '$ONLY_CHANGED_SINCE_SHA' - not found in git history"
    exit 1
  fi

  # use git to find files changed since the specified SHA
  echo "Checking Kotlin files changed since $ONLY_CHANGED_SINCE_SHA..."
  files=$(git diff --name-only --diff-filter=ACMRT "$ONLY_CHANGED_SINCE_SHA" -- '*.kt' '*.kts' 2>/dev/null)
  
  # Remove duplicate lines and empty lines
  unique_files=$(echo "$files" | sort | uniq | grep -v '^$')
  
  if [[ -z "$unique_files" ]]; then
    echo "No Kotlin files have been modified since $ONLY_CHANGED_SINCE_SHA."
    exit 0
  fi
  
  # Run ktfmt check on the modified files
  errors=$(echo "$unique_files" | PATH="$PATH" xargs -n "$KTFMT_BATCH_SIZE" -P "$(nproc 2>/dev/null || echo 4)" ktfmt --kotlinlang-style --dry-run 2>&1)
elif [[ "${ONLY_TOUCHED_FILES}" == "true" ]]; then
  # use git so we are only checking changed kotlin source files
  echo "Checking only git touched Kotlin files..."
  # Get all changed/added/modified Kotlin files
  files=$(git diff --name-only --diff-filter=ACMRT --cached -- '*.kt' '*.kts' 2>/dev/null)
  files+=$'\n'$(git diff --name-only --diff-filter=ACMRT HEAD -- '*.kt' '*.kts' 2>/dev/null)
  
  # Remove duplicate lines and empty lines
  unique_files=$(echo "$files" | sort | uniq | grep -v '^$')
  
  if [[ -z "$unique_files" ]]; then
    echo "No Kotlin files have been modified."
    exit 0
  fi
  
  # Run ktfmt check on the modified files
  errors=$(echo "$unique_files" | PATH="$PATH" xargs -n "$KTFMT_BATCH_SIZE" -P "$(nproc 2>/dev/null || echo 4)" ktfmt --kotlinlang-style --dry-run 2>&1)
else
  # simply apply ktfmt to all kotlin source files
  echo "Checking all Kotlin files in the project..."
  
  # Find all Kotlin files
  files=$(find . -type f \( -name "*.kt" -o -name "*.kts" \) -not -path "*/build/*" -not -path "*/.gradle/*")
  
  if [[ -z "$files" ]]; then
    echo "No Kotlin files found in the project."
    exit 0
  fi
  
  # Run ktfmt check on all Kotlin files
  errors=$(echo "$files" | PATH="$PATH" xargs -n "$KTFMT_BATCH_SIZE" -P "$(nproc 2>/dev/null || echo 4)" ktfmt --kotlinlang-style --dry-run 2>&1)
fi

# Calculate total elapsed time
end_time=$(bash -c "$(pwd)/ci/get_timestamp.sh")
total_elapsed=$((end_time - start_time))

# Newer JDKs print sun.misc.Unsafe deprecation warnings when ktfmt starts; they are not
# formatting errors.
errors=$(echo "$errors" | grep -v '^WARNING: ' || true)

# Check and report errors
if [[ -n $errors ]]; then
    echo "Errors in the following files:"
    echo "$errors"
    echo "Total time elapsed: $total_elapsed ms."
    exit 1
fi

echo "All Kotlin source files pass ktfmt checks."
