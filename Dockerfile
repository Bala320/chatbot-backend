# Stage 1 - Build
FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /app

# Optimization: Copy only pom.xml first to cache dependencies
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline

# Copy source and build
COPY src ./src
RUN ./mvnw clean package -DskipTests

# Stage 2 - Run
# Using 'jammy' (Ubuntu 22.04) or 'noble' (24.04) ensures latest security patches
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Professional move: Run as a non-root user for security
RUN useradd -ms /bin/bash springuser
USER springuser

# Copy the jar from the builder stage
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

# Use optimized JVM flags for containers
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]