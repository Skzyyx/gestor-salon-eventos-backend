# ---- Stage 1: build ----
# Full JDK + Maven, only to compile. Nothing from this stage reaches the final image
# except the jar copied below.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# pom.xml first: while dependencies do not change, Docker reuses this layer and
# does not download them again on every code change.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
# Tests are skipped on purpose: CI already ran them before this image is built.
RUN mvn -B -q package -DskipTests

# ---- Stage 2: runtime ----
# JRE only (no compiler, no Maven, no source code) on Alpine.
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Run as an unprivileged user, not root.
RUN addgroup -S app && adduser -S app -G app

# target/ holds gestor-salon-eventos-<version>.jar and the .jar.original that
# Spring Boot leaves behind; *.jar only matches the executable one.
COPY --from=build /app/target/*.jar app.jar

USER app
EXPOSE 8080
# Inside a container everything comes from environment variables (application-prod.yml).
ENV SPRING_PROFILES_ACTIVE=prod
ENTRYPOINT ["java", "-jar", "app.jar"]