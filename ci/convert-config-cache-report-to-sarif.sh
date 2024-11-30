#!/bin/bash

# Ensure required tools are installed
if ! command -v jq &> /dev/null; then
    echo "Please install jq to run this script."
    exit 1
fi

# Check arguments
if [ "$#" -ne 2 ]; then
    echo "Usage: $0 <input.html> <output.sarif>"
    exit 1
fi

INPUT_FILE="$1"
CLEANED_FILE="/tmp/config-cache-clean.html"
OUTPUT_FILE="$2"

tail -n 11 "$INPUT_FILE" | head -n 1 | jq . > "$CLEANED_FILE"

# Initialize SARIF structure
# shellcheck disable=SC2016
sarif_template='{
    "$schema": "https://raw.githubusercontent.com/oasis-tcs/sarif-spec/master/Schemata/sarif-schema-2.1.0.json",
    "version": "2.1.0",
    "runs": []
}'
sarif=$(echo "$sarif_template" | jq '.')

iterator=0

# Process diagnostics into SARIF results
results=()
while true; do

  diagnostic=$(jq -c ".diagnostics[$iterator]" "$CLEANED_FILE")

  if [[ "$diagnostic" == "null" ]]; then
    # Add results to SARIF
    for result in "${results[@]}"; do
      sarif=$(echo "$sarif" | jq ".runs[0].results += [$result]")
    done

    # Write SARIF to output file
    echo "$sarif" | jq '.' > "$OUTPUT_FILE"
    echo "SARIF report written to $OUTPUT_FILE"
    exit 0
  fi

  any_problem=$(echo "$diagnostic" | jq -r '.problem')
  any_error=$(echo "$diagnostic" | jq -r '.error.summary')

  if [[ "$any_problem" != "null" && "$any_error" != "null" ]]; then
    problem_text=$(echo "$diagnostic" | jq -r '.problem[] | if has("text") then .text else .name end' | tr '\n' '\0')
    error_summary=$(echo "$diagnostic" | jq -r '.error.summary[] | if has("text") then .text else .name end' | tr '\n' '\0')

    rule_id=$(echo -n "$problem_text" | md5sum | awk '{print $1}')
    severity="error"

    rule=$(jq -n --arg id "$rule_id" --arg text "$problem_text" '{
        id: $id,
        shortDescription: { text: $text },
        help: { text: $text, markdown: $text }
    }')

    result=$(jq -n --arg id "$rule_id" --arg sev "$severity" --arg msg "$problem_text" --arg loc "$error_summary" '{
        ruleId: $id,
        level: $sev,
        message: { text: $msg },
        locations: [
            {
                physicalLocation: {
                    artifactLocation: { uri: $loc }
                }
            }
        ]
    }')

    results+=("$result")
    sarif=$(echo "$sarif" | jq ".runs[0].tool.driver.rules += [$rule]")
  fi
  iterator=$((iterator+1))
done
