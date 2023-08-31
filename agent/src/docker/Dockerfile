# --- build image ---

FROM openjdk:8 as build
ADD . /src
WORKDIR /src
# Workaround for a bug in the Gradle shadow plugin that attempt to transform artifacts too early
RUN ./gradlew :teamscale-client:jar :report-generator:jar && ./gradlew :agent:assemble

# --- production image ---

FROM alpine:latest

RUN mkdir -p /agent
COPY --from=build /src/agent/build/libs/teamscale-jacoco-agent.jar /agent

VOLUME /agent

# make the image do nothing when started
CMD exec /bin/sh -c "trap : TERM INT; sleep infinity & wait"
