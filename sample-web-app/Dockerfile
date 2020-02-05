FROM openjdk:11-jre-slim
ADD build/distributions/sample-web-app.tar /usr/local/
COPY docker-jacoco.config /usr/local/agent/
COPY docker-logging.xml /usr/local/agent/
EXPOSE 8081
EXPOSE 8123
ENTRYPOINT [ "/usr/local/sample-web-app/bin/sample-web-app" ]
