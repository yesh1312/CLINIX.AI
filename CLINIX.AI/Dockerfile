# Build stage
FROM maven:3.8-openjdk-17-slim AS build
WORKDIR /app
COPY pom.xml .
# Copy only the files needed for dependency resolution first (optimization)
RUN mvn dependency:go-offline

COPY src ./src
# Build the JAR, skipping tests for faster deployment
RUN mvn clean package -DskipTests

# Run stage
FROM openjdk:17-slim
WORKDIR /app
# Copy the built JAR from the build stage
COPY --from=build /app/target/*.jar app.jar

# Dynamic port assignment for Render
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar", "--server.port=${PORT:-8080}"]
