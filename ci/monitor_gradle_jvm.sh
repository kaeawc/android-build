#!/bin/bash

# Set the Grafana Cloud Prometheus remote write URL and API Key
# shellcheck disable=SC2034
GRAFANA_CLOUD_URL=""
GRAFANA_USER=""
GRAFANA_API_KEY=""

# Check if jstat is available
if ! command -v jstat &> /dev/null; then
    echo "jstat could not be found. Please install it before running this script."
    exit 1
fi

# Ensure jcmd is available
if ! command -v jcmd &> /dev/null; then
    echo "jcmd could not be found. Please install it before running this script."
    exit 1
fi

# Find the Gradle daemon process ID
GRADLE_PID=$(pgrep -f '.*GradleDaemon.*' | xargs -I{} ps -o pid= -o lstart= -p {} | sort -k2,3 | tail -n 1 | awk '{print $1}')

if [ -z "$GRADLE_PID" ]; then
    echo "No Gradle daemon process found. Please make sure a Gradle daemon is running."
    exit 1
fi

echo "Monitoring Gradle Daemon (PID: $GRADLE_PID)"

# Function to convert size with units (KB, MB) into bytes
convert_to_bytes() {
    local value=$1
    local unit=$2

    case $unit in
        KB)
            printf "%.0f" "$(echo "$value * 1024" | bc)"
            ;;
        k)
            printf "%.0f" "$(echo "$value * 1024" | bc)"
            ;;
        K)
            printf "%.0f" "$(echo "$value * 1024" | bc)"
            ;;
        MB)
            printf "%.0f" "$(echo "$value * 1024 * 1024" | bc)"
            ;;
        m)
            printf "%.0f" "$(echo "$value * 1024 * 1024" | bc)"
            ;;
        M)
            printf "%.0f" "$(echo "$value * 1024 * 1024" | bc)"
            ;;
        *)
            
            printf "%.0f" "$value"  # Already in bytes, ensure it's an integer
            ;;
    esac
}


# Function to extract the used and committed space for a segment
extract_code_cache_segment_data() {
  local segment_name=$1
  local code_cache_output=$2
  # shellcheck disable=SC2155
  local used_space=$(echo "$code_cache_output" | sed -n "/Global CodeHeap statistics for segment CodeHeap '$segment_name'/,/Verifying collected data/p" | grep 'usedSpace' | awk '{print $3}' | head -1)

  # shellcheck disable=SC2155
  local used_space_value=$(echo "$used_space" | grep -o -E '[0-9]+')

  # shellcheck disable=SC2155
  local used_space_unit=$(echo "$used_space" | grep -o -E '[^0-9, ]+')

  # shellcheck disable=SC2155
  local commited_size=$(echo "$code_cache_output" | sed -n "/C O D E   H E A P   A N A L Y S I S   (used blocks) for segment CodeHeap '$segment_name'/,/Verifying collected data/p" | grep "CodeHeap committed size" | awk '{print $4}' | head -1)

  # shellcheck disable=SC2155
  local commited_size_value=$(echo "$commited_size" | grep -o -E '[0-9]+')

  # shellcheck disable=SC2155
  local commited_size_unit=$(echo "$commited_size" | grep -o -E '[^0-9, ]+')

  # We do not double quote here because we want this behavior
  # shellcheck disable=SC2086
  echo "$(convert_to_bytes $used_space_value $used_space_unit)" "$(convert_to_bytes $commited_size_value $commited_size_unit)"
}

# Function to push metaspace metrics to Grafana Cloud Prometheus
push_metrics() {

    # Metaspace
    metaspace_data=$(jcmd "$GRADLE_PID" VM.metaspace)
    non_class_line=$(echo "$metaspace_data" | grep ' Non-Class: ' | head -n 1)
    non_class_metaspace_used=$(echo "$non_class_line" | awk '{print $11, $12}')
    class_line=$(echo "$metaspace_data" | grep ' Class: ' | head -n 1)
    class_metaspace_used=$(echo "$class_line" | awk '{print $11, $12}')

    non_class_value=$(echo "$non_class_metaspace_used" | awk '{print $1}')
    non_class_unit=$(echo "$non_class_metaspace_used" | awk '{print $2}')
    non_class_metaspace_used_bytes=$(convert_to_bytes "$non_class_value" "$non_class_unit")

    class_value=$(echo "$class_metaspace_used" | awk '{print $1}')
    class_unit=$(echo "$class_metaspace_used" | awk '{print $2}')
    class_metaspace_used_bytes=$(convert_to_bytes "$class_value" "$class_unit")

    # Heap
    heap_data=$(jstat -gc "$GRADLE_PID" | awk 'NR==2 {print "Eden Space: " $6*1024 "\nSurvivor Space: " $3*1024 + $4*1024 "\nOld Generation: " $8*1024}')

    eden_line=$(echo "$heap_data" | grep 'Eden Space: ' | head -n 1)
    eden_value=$(echo "$eden_line" | awk '{print $3}')
    eden_bytes=$(convert_to_bytes "$eden_value" "")
    survivor_line=$(echo "$heap_data" | grep 'Survivor Space: ' | head -n 1)
    survivor_value=$(echo "$survivor_line" | awk '{print $3}')
    survivor_bytes=$(convert_to_bytes "$survivor_value" "")
    old_line=$(echo "$heap_data" | grep 'Old Generation: ' | head -n 1)
    old_value=$(echo "$old_line" | awk '{print $3}')
    old_bytes=$(convert_to_bytes "$old_value" "")

    # CodeCache
    codecache_analysis=$(jcmd "$GRADLE_PID" Compiler.CodeHeap_Analytics)
    non_profiled_data=$(extract_code_cache_segment_data 'non-profiled nmethods' "$codecache_analysis")
    profiled_data=$(extract_code_cache_segment_data 'profiled nmethods' "$codecache_analysis")
    non_nmethods_data=$(extract_code_cache_segment_data 'non-nmethods' "$codecache_analysis")

    non_profiled_used=$(echo "$non_profiled_data" | awk '{print $1}')
    non_profiled_committed=$(echo "$non_profiled_data" | awk '{print $2}')
    non_profiled_used_value=$(echo "$non_profiled_used" | grep -o -E '[0-9]+')
    non_profiled_committed_value=$(echo "$non_profiled_committed" | grep -o -E '[0-9]+')

    profiled_used=$(echo "$profiled_data" | awk '{print $1}')
    profiled_committed=$(echo "$profiled_data" | awk '{print $2}')
    profiled_used_value=$(echo "$profiled_used" | grep -o -E '[0-9]+')
    profiled_committed_value=$(echo "$profiled_committed" | grep -o -E '[0-9]+')

    # shellcheck disable=SC2086
    non_nmethods_used=$(echo $non_nmethods_data | awk '{print $1}')
    non_nmethods_committed=$(echo "$non_nmethods_data" | awk '{print $2}')
    non_nmethods_used_value=$(echo "$non_nmethods_used" | grep -o -E '[0-9]+')
    non_nmethods_committed_value=$(echo "$non_nmethods_committed" | grep -o -E '[0-9]+')

    code_cache_used_value=$((non_profiled_used_value + profiled_used_value + non_nmethods_used_value))
    code_cache_committed_value=$((non_profiled_committed_value + profiled_committed_value + non_nmethods_committed_value))

    # Get the current timestamp in milliseconds
    current_timestamp=$(date +%s)

    # Prepare Prometheus metrics format
    # shellcheck disable=SC2034
    METRICS=$(cat <<EOF
# HELP jvm_class_space_used_bytes JVM class space used (in bytes)
# TYPE jvm_class_space_used_bytes gauge
jvm_class_space_used_bytes $class_metaspace_used_bytes $current_timestamp
# HELP jvm_non_class_metaspace_used_bytes JVM non-class metaspace used (in bytes)
# TYPE jvm_non_class_metaspace_used_bytes gauge
jvm_non_class_metaspace_used_bytes $non_class_metaspace_used_bytes $current_timestamp
# HELP jvm_eden_space_used JVM Eden space used (in bytes)
# TYPE jvm_eden_space_used gauge
jvm_eden_space_used $eden_bytes $current_timestamp
# HELP jvm_survivor_space_used JVM Survivor space used (in bytes)
# TYPE jvm_survivor_space_used gauge
jvm_survivor_space_used $survivor_bytes $current_timestamp
# HELP jvm_old_gen_space_used JVM Old Generation space used (in bytes)
# TYPE jvm_old_gen_space_used gauge
jvm_old_gen_space_used $old_bytes $current_timestamp
# HELP jvm_code_cache_non_profiled_used Description (in bytes)
# TYPE jvm_code_cache_non_profiled_used gauge
jvm_code_cache_non_profiled_used $non_profiled_used_value $current_timestamp
# HELP jvm_code_cache_profiled_used Description (in bytes)
# TYPE jvm_code_cache_profiled_used gauge
jvm_code_cache_profiled_used $profiled_used_value $current_timestamp
# HELP jvm_code_cache_non_nmethods_used Description (in bytes)
# TYPE jvm_code_cache_non_nmethods_used gauge
jvm_code_cache_non_nmethods_used $non_nmethods_used_value $current_timestamp
# HELP jvm_code_cache_total_used Description (in bytes)
# TYPE jvm_code_cache_total_used gauge
jvm_code_cache_total_used $code_cache_used_value $current_timestamp
EOF
)

    # TODO: Still iterating on Grafana/Prometheus setup
    # echo "$METRICS" | snzip -c > metrics.snappy

    # # Send the metrics to Grafana Cloud Prometheus using curl
    # curl -X POST "$GRAFANA_CLOUD_URL" \
    #      -u "$GRAFANA_USER:$GRAFANA_API_KEY" \
    #      --header "Content-Type: text/plain" \
    #      --header "Content-Encoding: identity" \
    #      --data-binary "$METRICS"

}

# Interval to check memory usage (in seconds)
INTERVAL=1

# Continuously monitor the memory usage
while true; do
    push_metrics
    sleep $INTERVAL
done