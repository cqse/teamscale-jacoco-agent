#!/bin/sh
curl -X POST http://localhost:8123/test/start/myTests%2fDefault
curl http://localhost:8081/hello
curl -X POST -H "Content-Type: application/json" --data '{"result":"PASSED","message":"Test 1"}' http://localhost:8123/test/end/myTests%2fDefault

curl -X POST http://localhost:8123/test/start/myTests%2fCustom
curl http://localhost:8081/Custom
curl -X POST -H "Content-Type: application/json" --data '{"result":"PASSED","message":"Test 2"}' http://localhost:8123/test/end/myTests%2fCustom

tmp/teamscale-jacoco-agent/bin/convert --classDir tmp/sample-web-app/lib/sample-web-app.jar --in tmp/output --testwise-coverage --out tmp/testwise.json

