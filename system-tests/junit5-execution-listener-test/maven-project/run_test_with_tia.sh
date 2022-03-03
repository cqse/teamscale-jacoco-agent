#!/bin/bash
set -e
set -x
tree || true
ls -al || true
./mvnw clean test

