#!/bin/bash
profiler_dist="../agent/build/distributions/teamscale-jacoco-agent.zip"

# Please comment out in case you don't need to build either of these
../gradlew :agent:assemble
../gradlew :sample-app:assemble

rm -rf tmp
unzip -d tmp "$profiler_dist"
unzip -d tmp build/distributions/sample-app.zip
JAVA_TOOL_OPTIONS="-javaagent:tmp/teamscale-jacoco-agent/lib/teamscale-jacoco-agent.jar=config-file=./config.properties" tmp/sample-app/bin/sample-app
