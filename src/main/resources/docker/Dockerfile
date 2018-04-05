from openjdk:8

add . /client

# configurable options:
env \
# - the directory where the application class files/jars/wars/... are mounted (using volumes_from)
    CLASSES_DIR=/classes \
# - the hostname under which the application is running
    HOSTNAME=app \
# - the JaCoCo port on that host to which the tool should connect
    PORT=9876 \
# - the amount of time to wait between dumping coverage (in seconds)
    DUMP_INTERVAL=3600 \
# - include filters for class files to process (defaults to no filtering)
    INCLUDE_FILTERS="**" \
# - exclude filters for class files to process (defaults to no filtering)
    EXCLUDE_FILTERS="does-not-exist" \
# - the amount of time to wait before trying to reconnect after the application shut down/became unreachable
    RECONNECT_INTERVAL=300 \
# - whether to ignore the "duplicate class files" error (use with care, can lead to wrong coverage results)
    IGNORE_DUPLICATE_CLASS_FILES="false" \
# - the log4j2 logging configuration
    LOG_CONFIG=/client/log4j2.xml \
# - the amount of time to wait for the application to come up before starting the client
    MAX_WAIT_TIME=30

# create the volumes where the output of the tools is written
# should be persisted somewhere, e.g. on the host
run mkdir -p /output/traces /output/logs
volume /output/traces /output/logs

workdir /client
cmd exec /bin/bash ./wait-for-it.sh -t "$MAX_WAIT_TIME" -h "$HOSTNAME" -p "$PORT" -- java -Dlog4j.configurationFile="$LOG_CONFIG" -jar teamscale-jacoco-client.jar \
    watch -c "$CLASSES_DIR" -i "$DUMP_INTERVAL" -r "$RECONNECT_INTERVAL" -o /output/traces -h "$HOSTNAME" -p "$PORT" \
    -f "$INCLUDE_FILTERS" -e "$EXCLUDE_FILTERS" -d "$IGNORE_DUPLICATE_CLASS_FILES"

