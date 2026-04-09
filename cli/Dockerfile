# Multi-stage build: gradle shadowJar in stage 1, slim runtime in stage 2.
# The native libvalengine.so is expected to be mounted at /native at runtime
# (see docker-compose.yml).
FROM eclipse-temurin:21-jdk-jammy AS builder
WORKDIR /app
COPY settings.gradle.kts build.gradle.kts gradlew ./
COPY gradle gradle
RUN ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true
COPY src src
RUN ./gradlew --no-daemon shadowJar

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=builder /app/build/libs/*-all.jar /app/lab-watcher.jar
ENV JAVA_LIBRARY_PATH=/native
ENTRYPOINT ["java", "-Djava.library.path=/native", "-jar", "/app/lab-watcher.jar"]
CMD ["watch", "/data", "--config", "/app/lab-watcher.toml"]
