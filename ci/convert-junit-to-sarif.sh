#!/bin/bash

# Converts a JUnit XML report to SARIF format (schema 2.1.0)

if [ "$#" -ne 2 ]; then
  echo "Usage: $0 <junit-report.xml> <output.sarif>"
  exit 1
fi

INPUT_FILE="$1"
OUTPUT_FILE="$2"
#!/bin/bash

# Required tools: xmlstarlet, jq
# Ensure required tools are installed
if ! command -v xmlstarlet &> /dev/null || ! command -v jq &> /dev/null; then
    echo "Please install xmlstarlet and jq to run this script."
    exit 1
fi

# Initialize SARIF structure
# shellcheck disable=SC2016
sarif_template='{
    "$schema": "https://raw.githubusercontent.com/oasis-tcs/sarif-spec/master/Schemata/sarif-schema-2.1.0.json",
    "version": "2.1.0",
    "runs": []
}'
sarif=$(echo "$sarif_template" | jq '.')


# Helper functions
sarif_problem_severity() {
    case "$1" in
        "High") echo "error" ;;
        "Medium") echo "warning" ;;
        "Low") echo "note" ;;
        "Information") echo "none" ;;
        *) echo "none" ;;
    esac
}

sarif_security_severity() {
    case "$1" in
        "High"|"Medium"|"Low") echo "0.0" ;;
        "Information") echo "1.0" ;;
        *) echo "1.0" ;;
    esac
}

# Parse XML
testsuite=$(xmlstarlet sel -t -m "//testsuite" -v "concat(@name, '|', @failures)" -n "$INPUT_FILE")

# Process testsuite
IFS='|' read -r testsuite_name testsuite_failures <<< "$testsuite"
if [[ "$testsuite_failures" -gt 0 ]]; then
    testcases=$(xmlstarlet sel -t -m "//testsuite/testcase" -v "concat(@name, '|', @type, '|', failure/@message, '|', failure)" -n "$INPUT_FILE")

    # Prepare run object
    run='{
        "tool": {
            "driver": {
                "name": "Test Reporter",
                "version": "1.0",
                "informationUri": "https://github.com/kaeawc/android-build",
                "rules": []
            }
        },
        "originalUriBaseIds": {
            "target": {
                "uri": "PLACEHOLDER",
                "description": {
                    "text": "The base URI for all test artifacts."
                }
            }
        },
        "results": []
    }'
    echo "parsing run for originalUriBaseIds"
    run=$(echo "$run" | jq ".originalUriBaseIds.target.uri = \"$testsuite_name/\"")
    echo "done with originalUriBaseIds"

    rule_index=0
    while IFS='|' read -r testcase_name testcase_type failure_message failure_text; do
        if [[ -z "$failure_message" ]]; then
            continue
        fi

        # Generate rule ID
        rule_id=$(echo -n "${testsuite_name}${failure_message}" | md5sum | awk '{print $1}')
        severity=$(sarif_problem_severity "$testcase_type")
        security_severity=$(sarif_security_severity "$testcase_type")

        # Define rule
        echo "defining rule"
        rule=$(jq -n --arg id "$rule_id" --arg text "$failure_message" --arg severity "$severity" --arg sec_sev "$security_severity" '{
            id: $id,
            shortDescription: { text: $text },
            help: { text: $text, markdown: "# " + $text },
            properties: {
                impact: [$text],
                "problem.severity": $severity,
                resolution: [$text],
                "security-severity": $sec_sev
            }
        }')

        # Add rule to run
        echo "Add rule to run"
        run=$(echo "$run" | jq ".tool.driver.rules += [$rule]")
        echo "done with rule"

        # Define result
        result=$(jq -n --arg id "$rule_id" --arg idx "$rule_index" --arg sev "$severity" --arg msg "$failure_message" --arg uri "$testcase_name" '{
            ruleId: $id,
            ruleIndex: ($idx | tonumber),
            level: $sev,
            message: { text: $msg },
            locations: [
                {
                    physicalLocation: {
                        artifactLocation: { uri: $uri, uriBaseId: "target" }
                    }
                }
            ]
        }')

        # Add result to run
        echo "Add result to run"
        run=$(echo "$run" | jq ".results += [$result]")
        echo "Done with result"

        rule_index=$((rule_index + 1))
    done <<< "$testcases"

    # Add run to SARIF
    echo "Add run to SARIF"
    sarif=$(echo "$sarif" | jq ".runs += [$run]")
    echo "done with run"
fi

# Write SARIF to output file
echo "$sarif" | jq '.' > "$OUTPUT_FILE"
echo "SARIF report written to $OUTPUT_FILE"
