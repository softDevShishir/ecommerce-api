# Uses the official Maven image (bundles Maven + JDK 21) since this repo does
# not commit a Maven wrapper (mvnw / .mvn).
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# curl is required for the HEALTHCHECK below; Alpine doesn't ship it by default.
RUN apk add --no-cache curl

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

# server.servlet.context-path is /api/v1 (see application.yml), so the probe
# path must include it or every check 404s.
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/api/v1/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
