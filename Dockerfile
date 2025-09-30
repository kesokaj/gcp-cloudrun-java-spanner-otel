# STAGE 1: Build the application using a Maven image
# This is the "builder" stage.
FROM maven:3.9-eclipse-temurin-17 AS builder

# Set the Current Working Directory inside the container
WORKDIR /app

# Copy the pom.xml file to download dependencies first, leveraging Docker's cache.
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy the rest of the source code
COPY src ./src

# Build the application, creating a single executable JAR file.
# The shade plugin configured in pom.xml will handle packaging dependencies.
RUN mvn package


# STAGE 2: Create the final, minimal image
# "distroless" images contain only the application and its runtime dependencies.
# They do not contain package managers, shells, or any other programs.
# We use the java11 version to match our compilation target.
FROM gcr.io/distroless/java17-debian12

# Set the Current Working Directory inside the container
WORKDIR /app

# Copy the Pre-built JAR file from the builder stage
COPY --from=builder /app/target/run-java-spanner-demo-1.0-SNAPSHOT.jar .

# Command to run the executable
# The application will start and listen for configuration via environment variables.
ENTRYPOINT ["java", "-jar", "run-java-spanner-demo-1.0-SNAPSHOT.jar"]
