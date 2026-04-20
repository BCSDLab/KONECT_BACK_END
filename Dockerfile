FROM amazoncorretto:21-alpine

WORKDIR /app

RUN addgroup -g 1000 -S konect \
 && adduser -u 1000 -S konect -G konect \
 && mkdir -p /app /app/logs \
 && chown -R 1000:1000 /app \
 && chmod 755 /app /app/logs

COPY --chown=1000:1000 build/libs/KONECT_API.jar KONECT_API.jar

USER 1000:1000

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", \
  "-Xms128m", \
  "-Xmx256m", \
  "-XX:MaxMetaspaceSize=256m", \
  "-XX:+UseStringDeduplication", \
  "-Xlog:gc*:file=/app/logs/gc.log:time,uptime,level,tags:filecount=5,filesize=10m", \
  "-XX:+HeapDumpOnOutOfMemoryError", \
  "-XX:HeapDumpPath=/app/logs/heapdump.hprof", \
  "-jar", "KONECT_API.jar"]
