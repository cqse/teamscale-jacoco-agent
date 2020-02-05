# --- build image ---

FROM gradle:6.1-jdk8 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle :agent:shadowJar --no-daemon

# --- production image ---

FROM alpine:latest

RUN mkdir -p /agent
COPY --from=build /home/gradle/src/agent/build/libs/teamscale-jacoco-agent-15.2.0.jar /agent/

VOLUME /agent

# make the image do nothing when started
CMD /bin/sh -c "trap ':' TERM INT; sleep 3600 & wait"