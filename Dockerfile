# Stage 1: Build
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app

# Copy gradle files first to cache dependencies
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# Grant execution permission and download dependencies
RUN chmod +x gradlew
RUN ./gradlew dependencies --no-daemon

# Copy source code and build
COPY src src
RUN ./gradlew bootJar --no-daemon

# Stage 2: Run
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Copy the built jar from the build stage
COPY --from=build /app/build/libs/*.jar app.jar

# Set default profile to prod
ENV SPRING_PROFILES_ACTIVE=prod

# Expose the application port
EXPOSE 10001

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
