#!/bin/bash
./gradlew publish \
  -Psigning.secretKeyRingFile=.gnupg/secring.gpg \
  -Psigning.password=$MAVEN_CENTRAL_GPG \
  -Psigning.keyId=4FB80B8E \
  -PsonatypeUsername=cqse-build-guild \
  -PsonatypePassword=$SONATYPE_PW