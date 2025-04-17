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

# Set the default PORT environment variable so the application can read it.
ENV PORT=8080
ENV GRPC_PORT=50051

# Copy the packaged JAR from the build stage
COPY --from=build /app/target/cache-pipeline-0.0.1-SNAPSHOT.jar app.jar

# Expose the port defined by the PORT env variable (cannot use env in EXPOSE, so we use the default value)
EXPOSE 8080
EXPOSE 50051

# Run the application
CMD ["java", "-jar", "app.jar"]
