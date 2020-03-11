#!/bin/bash
profiler_dist="../agent/build/distributions/teamscale-jacoco-agent.zip"
rm -r tmp
unzip -d tmp "$profiler_dist"
unzip -d tmp build/distributions/sample-web-app.zip

JAVA_TOOL_OPTIONS="-javaagent:tmp/teamscale-jacoco-agent/lib/teamscale-jacoco-agent.jar=config-file=jacoco.config" tmp/sample-web-app/bin/sample-web-app
