#!/usr/bin/env bash

# Check if shellcheck is installed
if ! command -v shellcheck &>/dev/null; then
    echo "shellcheck missing"
    if [[ "$OSTYPE" == "darwin"* ]]; then
      echo "Try 'brew install shellcheck'"
    else
      echo "Consult your OS package manager"
    fi
    exit 1
fi

# Start the timer
start_time=$(bash -c "$(pwd)/ci/get_timestamp.sh")

errors=""

# Find shell scripts and validate in parallel
git ls-files --cached --others --exclude-standard -z |
  grep -z '\.sh$' |
  xargs -0 -n 16 -P "$(nproc)" bash -c 'shellcheck "$0"' 2>&1 |
  tee >(errors+=cat)

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

echo "All shell scripts are valid."
