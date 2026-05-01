# Build stage
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
# Copy only the files needed for dependency resolution first (optimization)
RUN mvn dependency:go-offline

COPY src ./src
# Build the JAR, skipping tests for faster deployment
RUN mvn clean package -DskipTests

# Run stage
# Using JRE instead of JDK for a smaller, more secure image
FROM eclipse-temurin:17-jre
WORKDIR /app
# Copy the built JAR from the build stage
COPY --from=build /app/target/*.jar app.jar

# Dynamic port assignment for Render
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar", "--server.port=${PORT:-8080}"]
