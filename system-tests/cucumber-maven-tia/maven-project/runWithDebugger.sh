#!/usr/bin/env bash
# Call this script to debug the impacted test engine (running within the forked JVM process of surefire).
# Use an IntelliJ remote run config and connect to the default JVM debugging port 5005
# Adapt the AGENT_VERSION to the one currently set in the `build.gradle.kts` in the root of this repo.
# TEAMSCALE_PORT can stay as is (just needs to be set at all).
AGENT_VERSION="32.1.0" TEAMSCALE_PORT=63800 ./mvnw -Dmaven.surefire.debug clean verify
