# Multi-stage build for SIGA IPT Spring Boot Backend
# Stage 1: Build JAR using Maven
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder

WORKDIR /app

# Cache Maven dependencies
COPY pom.xml ./
RUN mvn dependency:go-offline -B || true

# Copy source code and package application
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Minimal JRE 21 Runtime
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create non-root user for security best practices
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copy built artifact from builder stage
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
