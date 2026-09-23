# syntax=docker/dockerfile:1

# ---- Build stage: JDK 21 + Maven Wrapper, tests are run in CI ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

COPY mvnw pom.xml ./
COPY .mvn .mvn
# Guard against CRLF line endings when the repo was checked out on Windows
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw

COPY src src
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B -ntp package -DskipTests \
    && cp target/*.jar app.jar

# ---- Runtime stage: JRE 21, non-root user ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S qadam && adduser -S -G qadam qadam
COPY --from=build --chown=qadam:qadam /workspace/app.jar app.jar
USER qadam

EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
