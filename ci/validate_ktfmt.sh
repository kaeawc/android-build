#!/usr/bin/env bash

INSTALL_KTFMT_WHEN_MISSING=${INSTALL_KTFMT_WHEN_MISSING:-false}
ONLY_TOUCHED_FILES=${ONLY_TOUCHED_FILES:-true}

# Check if ktfmt is installed
if ! command -v ktfmt &>/dev/null; then
    echo "ktfmt missing"
    if [[ "${INSTALL_KTFMT_WHEN_MISSING}" == "true" ]]; then
      ci/install_ktfmt.sh
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

# Start the timer
start_time=$(bash -c "$(pwd)/ci/get_timestamp.sh")

# run ktfmt as efficiently as possible against all kt and kts source files
# use validate_shell_scripts as an example of how to collect error
if [[ "${ONLY_TOUCHED_FILES}" == "true" ]]; then
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
  errors=$(echo "$unique_files" | xargs -n 1 -P "$(nproc 2>/dev/null || echo 4)" ktfmt --kotlinlang-style --dry-run 2>&1)
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
  errors=$(echo "$files" | xargs -n 1 -P "$(nproc 2>/dev/null || echo 4)" ktfmt --kotlinlang-style --dry-run 2>&1)
fi

# Calculate total elapsed time
end_time=$(bash -c "$(pwd)/ci/get_timestamp.sh")
total_elapsed=$((end_time - start_time))

# Check and report errors
if [[ -n $errors ]]; then
    echo "Errors in the following files:"
    echo "$errors"
    echo "Total time elapsed: $total_elapsed ms."
    exit 1
fi

echo "All Kotlin source files pass ktfmt checks."
