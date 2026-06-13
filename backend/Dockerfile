# ==========================================
# Stage 1: Build Java Application
# ==========================================
FROM maven:3.9.8-eclipse-temurin-21-alpine AS builder
WORKDIR /app

# Copy pom.xml and download dependencies to leverage Docker layer caching
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build package
COPY src ./src
RUN mvn clean package -DskipTests

# ==========================================
# Stage 2: Minimal Runtime Environment
# ==========================================
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Add a non-root system user for security hardening
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copy executable jar from builder stage
COPY --from=builder /app/target/swiss-qrbill-enterprise-1.0.0.jar app.jar

# Expose Spring Boot's default port
EXPOSE 8081

# Optimize JVM flag settings for containerized execution
ENTRYPOINT ["java", "-XX:+UseG1GC", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
