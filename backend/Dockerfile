# syntax=docker/dockerfile:1

# ---- Stage 1: build (Maven + JDK 21) ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Copy only the POM first to leverage Docker layer caching for dependencies
COPY pom.xml .
RUN mvn -B -q dependency:go-offline || true

COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---- Stage 2: runtime (slim JRE 21) ----
FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app

# Non-root user for the running process
RUN groupadd -r spring && useradd -r -g spring spring \
    && mkdir -p /app/storage/pdfs \
    && chown -R spring:spring /app

COPY --from=build /build/target/*.jar /app/app.jar

USER spring
EXPOSE 8080
ENV JAVA_OPTS=""

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
