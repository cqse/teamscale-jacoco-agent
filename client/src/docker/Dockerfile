from alpine:latest

run mkdir -p /agent
add agent.jar /agent/agent.jar
volume /agent

# make the image do nothing when started
CMD /bin/sh -c "trap ':' TERM INT; sleep 3600 & wait"

