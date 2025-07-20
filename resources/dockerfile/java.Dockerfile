# Build stage
ARG BASE_IMAGE=openjdk:17-jdk-slim
ARG APP_NAME=my-java-app
ARG APP_VERSION=1.0.0

FROM ${BASE_IMAGE} AS builder

WORKDIR /app

# Install build tools if needed (e.g., Maven)
RUN apt-get update && apt-get install -y maven \
    && rm -rf /var/lib/apt/lists/*

# Copy source code
COPY . .

# Build the application
RUN mvn clean package -DskipTests

# Runtime stage
FROM ${BASE_IMAGE}

WORKDIR /app

# Create non-root user for security
RUN useradd -m appuser && chown -R appuser /app
USER appuser

# Copy built artifact from builder stage
COPY --from=builder /app/target/${APP_NAME}-${APP_VERSION}.jar app.jar

# Expose port (change if needed)
EXPOSE 8080

# Run with optimized JVM flags for containers
# Use ENV for configurable options
# Use percentage-based heap sizing for dynamic container memory
# Use equal percentage for initial and min/max heap
ENV JAVA_OPTS="-XX:InitialRAMPercentage=70.0 -XX:MinRAMPercentage=70.0 -XX:MaxRAMPercentage=70.0 -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -XX:+UseContainerSupport -jar app.jar"]