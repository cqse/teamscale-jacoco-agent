from openjdk:8

add . /client

# configurable options:
# - the directory where the application class files/jars/wars/... are mounted (using volumes_from)
# - the hostname under which the application is running
# - the JaCoCo port on that host to which the tool should connect
env CLASSES_DIR=/classes \
    HOSTNAME=app \
    PORT=9876 \
    DUMP_INTERVAL=3600 \
    INCLUDE_FILTERS="**" \
    EXCLUDE_FILTERS="does-not-exist" \
    RECONNECT_INTERVAL=300 \
    IGNORE_DUPLICATE_CLASS_FILES="false" \
    LOG_CONFIG=/client/log4j2.xml \
    MAX_WAIT_TIME=30

# create the volumes where the output of the tools is written
# should be persisted somewhere, e.g. on the host
run mkdir -p /output/traces /output/logs
volume /output/traces /output/logs

workdir /client
cmd /bin/bash ./wait-for-it.sh -t "$MAX_WAIT_TIME" -h "$HOSTNAME" -p "$PORT" -- java -Dlog4j.configurationFile="$LOG_CONFIG" -jar teamscale-jacoco-client.jar \
    watch -c "$CLASSES_DIR" -i "$DUMP_INTERVAL" -r "$RECONNECT_INTERVAL" -o /output/traces -h "$HOSTNAME" -p "$PORT" \
    -f "$INCLUDE_FILTERS" -e "$EXCLUDE_FILTERS" -d "$IGNORE_DUPLICATE_CLASS_FILES"

