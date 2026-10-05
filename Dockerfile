# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B -q

COPY src ./src
RUN mvn clean package -DskipTests -B -q

# ── Stage 2: Runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=builder /app/target/support-system-1.0.0.jar app.jar

# Render sets PORT env var — Spring Boot must bind on it
EXPOSE 8080

ENTRYPOINT ["java", "-Xms256m", "-Xmx512m", \
            "-Dserver.port=${PORT:-8080}", \
            "-Dserver.address=0.0.0.0", \
            "-jar", "app.jar"]
