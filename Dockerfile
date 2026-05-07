# syntax=docker/dockerfile:1.6

# ---- Build stage ------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Cache Maven dependencies in their own layer.
COPY pom.xml .
RUN mvn -B -q -e -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -B -q -e -DskipTests package

# ---- Runtime stage ----------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /build/target/aggregator.jar /app/aggregator.jar

# State is bounded by unique campaigns, not file size, so 128MB is plenty.
# G1GC is the default but we pin it explicitly for reproducibility.
ENTRYPOINT ["java", "-XX:+UseG1GC", "-Xmx128m", "-jar", "/app/aggregator.jar"]
