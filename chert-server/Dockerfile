FROM eclipse-temurin:25-jdk AS builder

WORKDIR /workspace

COPY gradle gradle
COPY gradlew gradlew
COPY settings.gradle.kts build.gradle.kts ./
COPY chert-client chert-client
COPY chert-server chert-server

RUN ./gradlew :chert-server:bootJar --no-daemon

FROM eclipse-temurin:25-jre

WORKDIR /app

COPY --from=builder /workspace/chert-server/build/libs/*.jar /app/chert-server.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/chert-server.jar"]
