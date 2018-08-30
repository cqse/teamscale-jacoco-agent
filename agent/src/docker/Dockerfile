FROM openjdk:8
ADD . /src
WORKDIR /src
RUN ls -la

FROM alpine:latest

RUN mkdir -p /agent
ADD agent.jar /agent/agent.jar
VOLUME /agent

# make the image do nothing when started
CMD /bin/sh -c "trap ':' TERM INT; sleep 3600 & wait"

