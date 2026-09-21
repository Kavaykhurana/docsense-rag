# syntax=docker/dockerfile:1
# DocSense backend image. Built from the repository root so the COPY paths below
# resolve against the monorepo. Multi-stage: Maven build -> slim JRE 21 runtime.

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
# Resolve dependencies first for better layer caching.
COPY backend/pom.xml ./pom.xml
RUN mvn -B -q dependency:go-offline
COPY backend/src ./src
RUN mvn -B -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd -m -u 10001 appuser
COPY --from=build /app/target/*.jar app.jar
USER appuser
# Render injects $PORT; Spring Boot reads ${PORT:8080} (see application.yml).
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
