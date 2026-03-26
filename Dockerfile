FROM amazoncorretto:21-alpine

ARG OTEL_JAVA_AGENT_VERSION=2.18.1

WORKDIR /app

RUN addgroup -S konect && adduser -S konect -G konect

COPY build/libs/KONECT_API.jar KONECT_API.jar

RUN wget -O opentelemetry-javaagent.jar "https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v${OTEL_JAVA_AGENT_VERSION}/opentelemetry-javaagent.jar"

RUN chown -R konect:konect /app

USER konect:konect

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_TOOL_OPTIONS -jar KONECT_API.jar"]
