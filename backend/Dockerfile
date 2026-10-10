# Multi-stage Dockerfile for DevTrack Backend
# Stage 1: Build & Package
FROM maven:3.9.8-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# Cache Maven dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build runnable artifact
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Minimal Production Runtime
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Install wget for container health checks
RUN apk add --no-cache wget

# Create non-privileged system user
RUN addgroup -S devtrack && adduser -S devtrack -G devtrack

# Copy executable jar from builder stage
COPY --from=builder /build/target/devtrack-backend-1.0.0.jar app.jar

RUN chown -R devtrack:devtrack /app

USER devtrack

EXPOSE 8081

HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8081/actuator/health || exit 1

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-XX:+ExitOnOutOfMemoryError", "-jar", "app.jar"]
