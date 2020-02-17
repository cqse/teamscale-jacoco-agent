#!/bin/sh
TEAMSCALE_URL=http://localhost:9570
TEAMSCALE_PROJECT=teamscale-jacoco-agent
AGENT_URL=http://localhost:8123
REPORT=tmp/testwise.json

# Appending to file for proof-of-concept. Productive use should employ
# proper JSON processing, e.g. using the `jq` command-line tool.
echo '{"tests":[' > $REPORT

# Save startup coverage as pseudo test case to get full line coverage
curl -X POST -H "Content-Type: application/json"\
  --data '{"result":"SKIPPED","message":"Startup"}'\
  "$AGENT_URL/test/end/startup" >> $REPORT

echo ',' >> $REPORT

# Test 1: Default message
curl -X POST "$AGENT_URL/test/start/myTests%2fDefault"
curl http://localhost:8081/hello
curl -X POST -H "Content-Type: application/json"\
  --data '{"result":"PASSED","message":"Test 1 (Default)"}'\
  "$AGENT_URL/test/end/myTests%2fDefault" >> $REPORT

echo ',' >> $REPORT

# Test 2: Custom message
curl -X POST "$AGENT_URL/test/start/myTests%2fCustom"
curl http://localhost:8081/Custom
curl -X POST -H "Content-Type: application/json"\
  --data '{"result":"PASSED","message":"Test 2 (Custom)"}'\
  "$AGENT_URL/test/end/myTests%2fCustom" >> $REPORT

echo '\n]}\n' >> $REPORT

# Upload to Teamscale
PROJECT_URL="$TEAMSCALE_URL/p/$TEAMSCALE_PROJECT"
curl -X POST -F "report=@$REPORT" -F "format=TESTWISE_COVERAGE"\
  "$PROJECT_URL/external-report?message=Testwise%20Coverage&partition=Testwise&movetolastcommit=true&t=tia_request_forwarding:HEAD"
