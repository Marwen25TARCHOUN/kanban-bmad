# syntax=docker/dockerfile:1.7
#
# Multistage backend build — AD-15.
# Build on maven:3.9-eclipse-temurin-21, run on eclipse-temurin:21-jre-alpine.
# Target image size: <250 MB.

# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Copy the pom first so dependency resolution is cached when only sources change.
COPY pom.xml ./
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline

COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q clean package -DskipTests
RUN cp target/*.jar /workspace/app.jar

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre-alpine AS runtime

# curl for the healthcheck; non-root user for a smaller attack surface.
RUN apk add --no-cache curl \
    && addgroup -S app \
    && adduser -S -G app app \
    && mkdir -p /data \
    && chown -R app:app /data

USER app
WORKDIR /opt/kanban

COPY --from=build --chown=app:app /workspace/app.jar ./app.jar

EXPOSE 8080
VOLUME ["/data"]

HEALTHCHECK --interval=10s --timeout=3s --start-period=20s --retries=5 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Respect the container memory limit (JVM ergonomics on Alpine).
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"

ENTRYPOINT ["java", "-jar", "/opt/kanban/app.jar"]
