FROM amazoncorretto:21-alpine

ARG OTEL_JAVA_AGENT_VERSION=2.18.1

WORKDIR /app

RUN addgroup -S konect && adduser -S konect -G konect

COPY build/libs/KONECT_API.jar KONECT_API.jar
COPY opentelemetry-javaagent.jar opentelemetry-javaagent.jar

RUN chown -R konect:konect /app

USER konect:konect

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", \
  "-Xms128m", \
  "-Xmx256m", \
  "-XX:MaxMetaspaceSize=128m", \
  "-XX:+UseStringDeduplication", \
  "-Xlog:gc*:file=/opt/konect/stage/api/logs:time,uptime,level,tags:filecount=5,filesize=10m", \
  "-XX:+HeapDumpOnOutOfMemoryError", \
  "-XX:HeapDumpPath=/app/heapdump.hprof", \
  "-jar", "KONECT_API.jar"]
