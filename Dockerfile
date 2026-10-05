FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B -q
COPY src ./src
RUN mvn clean package -DskipTests -B -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser
COPY --from=builder /app/target/support-system-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["sh","-c","java -Xms256m -Xmx512m -Dserver.port=${PORT:-8080} -Dserver.address=0.0.0.0 -jar app.jar"]
