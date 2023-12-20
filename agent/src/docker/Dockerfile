# --- build image ---

FROM openjdk:17 as build
ADD . /src
WORKDIR /src
RUN ./gradlew :agent:shadowJar

# --- production image ---

FROM alpine:latest

RUN mkdir -p /agent
COPY --from=build /src/agent/build/libs/teamscale-jacoco-agent.jar /agent

VOLUME /agent

# make the image do nothing when started
CMD exec /bin/sh -c "trap : TERM INT; sleep infinity & wait"
