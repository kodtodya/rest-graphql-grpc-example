# ─────────────────────────────────────────────────────────────────
#  Multi-stage Dockerfile
#  Stage 1: Build the JAR with Maven
#  Stage 2: Run with minimal JRE image
# ─────────────────────────────────────────────────────────────────

# ── Stage 1: Build ────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build

# Copy Maven wrapper and pom.xml first (layer caching — deps only
# re-downloaded when pom.xml changes, not on every source change)
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline -q

# Copy source and build
COPY src/ src/
RUN ./mvnw package -DskipTests -q

# ── Stage 2: Runtime ──────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# Non-root user for security
RUN addgroup -S showcase && adduser -S showcase -G showcase
USER showcase

# Copy built JAR from builder stage
COPY --from=builder /build/target/spring-api-showcase-1.0.0.jar app.jar

# Expose both ports: 8080 (REST + GraphQL) and 9090 (gRPC)
EXPOSE 8080 9090

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]