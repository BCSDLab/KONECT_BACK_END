FROM amazoncorretto:17-alpine

WORKDIR /app

RUN addgroup -S konect && adduser -S konect -G konect

COPY build/libs/KONECT_API.jar KONECT_API.jar

RUN chown konect:konect KONECT_API.jar

USER konect:konect

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "KONECT_API.jar"]
