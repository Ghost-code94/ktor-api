# Stage 1: Build the application using Maven with a JDK
FROM maven:3.9.0-eclipse-temurin-17 AS build
WORKDIR /app
# Cache pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B
# Copy the complete source code and build the jar, skipping tests
COPY src ./src
RUN mvn --batch-mode clean package -DskipTests

# Stage 2: Create a lightweight runtime image using a JRE
FROM eclipse-temurin:17-jre-alpine
# install Redis
RUN apk add --no-cache redis
WORKDIR /app

# Set the default PORT environment variable so the application can read it.
ENV GRPC_PORT=50051
ARG REDIS_URL
ENV REDIS_URL=$REDIS_URL
ARG JWT_SECRET
ENV JWT_SECRET=$JWT_SECRET

# Copy the packaged JAR from the build stage
COPY --from=build /app/target/cache-pipeline-0.0.1-SNAPSHOT.jar app.jar
COPY entrypoint.sh /entrypoint.sh

RUN chmod +x /entrypoint.sh

# Expose the port defined by the PORT env variable (cannot use env in EXPOSE, so we use the default value)
EXPOSE 50051

# single ENTRYPOINT that launches both Redis and your Ktor+gRPC service
ENTRYPOINT ["/entrypoint.sh"]