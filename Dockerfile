# Stage 1: Build the application using Maven with a JDK
FROM maven:3.9.0-eclipse-temurin-17-alpine AS build
WORKDIR /app
# Cache pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B
# Copy the complete source code and build the jar, skipping tests
COPY src ./src
RUN mvn --batch-mode clean package -DskipTests

# Stage 2: Create a lightweight runtime image using a JRE
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
# Copy the packaged JAR from the build stage
COPY --from=build /app/target/cache-pipeline-0.0.1-SNAPSHOT.jar app.jar
# Expose port 8080 (adjust if different)
EXPOSE 8080
# Run the application
CMD ["java", "-jar", "app.jar"]
