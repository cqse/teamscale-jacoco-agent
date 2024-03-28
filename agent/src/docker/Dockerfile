# --- build image ---

FROM openjdk:17 as build
# install xargs which is needed during the build
RUN microdnf install findutils
ADD . /src
WORKDIR /src
RUN ./gradlew :agent:shadowJar

# --- production image ---

FROM alpine:latest

RUN mkdir -p /agent
COPY --from=build /src/agent/build/libs/teamscale-jacoco-agent.jar /agent

VOLUME /agent

# make the image do nothing when started
# except if /transfer is mounted, then copy the agent there and exit immediately
# this lets us use the image as an init container in Kubernetes
CMD exec /bin/sh -c "[ -e /transfer ] && cp -r /agent/teamscale-jacoco-agent.jar /transfer && exit 0 ; trap : TERM INT; sleep infinity & wait"
