# Uses the official Maven image (bundles Maven + JDK 21) since this repo does
# not commit a Maven wrapper (mvnw / .mvn).
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app

COPY pom.xml .
COPY src ./src

# pom.xml pins compilation to a JDK 21 toolchain (see CLAUDE.md); this image's
# only JDK is 21, but maven-toolchains-plugin still needs a toolchains.xml
# entry pointing at it, which isn't present by default.
RUN mkdir -p /root/.m2 && printf '%s\n' \
  '<?xml version="1.0" encoding="UTF-8"?>' \
  '<toolchains>' \
  '  <toolchain>' \
  '    <type>jdk</type>' \
  '    <provides>' \
  '      <version>21</version>' \
  '    </provides>' \
  '    <configuration>' \
  "      <jdkHome>${JAVA_HOME}</jdkHome>" \
  '    </configuration>' \
  '  </toolchain>' \
  '</toolchains>' > /root/.m2/toolchains.xml

RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# curl is required for the HEALTHCHECK below; Alpine doesn't ship it by default.
RUN apk add --no-cache curl

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/swagger-ui.html || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
