# --- build image ---

FROM openjdk:8 as build
ADD . /src
WORKDIR /src
RUN ./gradlew :teamscale-jacoco-agent:assemble

# --- production image ---

FROM alpine:latest

RUN mkdir -p /agent
COPY --from=build /src/agent/build/libs/teamscale-jacoco-agent.jar /agent

VOLUME /agent

# make the image do nothing when started
CMD /bin/sh -c "trap ':' TERM INT; sleep 3600 & wait"