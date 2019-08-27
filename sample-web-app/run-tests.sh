#!/bin/sh
TEAMSCALE_URL=http://localhost:8530
TEAMSCALE_PROJECT=sample-web-app
AGENT_URL=http://localhost:8123

# Save startup coverage for jacoco report
curl -X POST "$AGENT_URL/test/end/startup"

# Test 1: Default message
curl -X POST "$AGENT_URL/test/start/myTests%2fDefault"
curl http://localhost:8081/hello
curl -X POST -H "Content-Type: application/json" --data '{"result":"PASSED","message":"Test 1 (Default)"}' "$AGENT_URL/test/end/myTests%2fDefault"

# Test 2: Custom message
curl -X POST "$AGENT_URL/test/start/myTests%2fCustom"
curl http://localhost:8081/Custom
curl -X POST -H "Content-Type: application/json" --data '{"result":"PASSED","message":"Test 2 (Custom)"}' "$AGENT_URL/test/end/myTests%2fCustom"

# Build and complete coverage (including startup) and testwise coverage
tmp/teamscale-jacoco-agent/bin/convert --classDir tmp/sample-web-app/lib/sample-web-app.jar --in tmp/output --out tmp/complete.xml
tmp/teamscale-jacoco-agent/bin/convert --classDir tmp/sample-web-app/lib/sample-web-app.jar --in tmp/output --testwise-coverage --out tmp/testwise.json

# Upload to Teamscale
PROJECT_URL="$TEAMSCALE_URL/p/$TEAMSCALE_PROJECT"
# SESSION=`curl -X GET "$PROJECT_URL/external-analysis?message=Coverage&partition=Coverage&movetolastcommit=true&t=tia_request_forwarding:HEAD"`
# Add -F "session=$SESSION" to below calls to enable session-based upload
curl -X POST -F "report=@tmp/complete.xml" -F "format=JACOCO" "$PROJECT_URL/external-report?message=Complete%20Coverage&partition=JaCoCo&movetolastcommit=true&t=tia_request_forwarding:HEAD"
curl -X POST -F "report=@tmp/testwise-1.json" -F "format=TESTWISE_COVERAGE" "$PROJECT_URL/external-report?message=Testwise%20Coverage&partition=Testwise&movetolastcommit=true&t=tia_request_forwarding:HEAD"
# curl -X POST "$PROJECT_URL/external-analysis/$SESSION"
