FROM eclipse-temurin:17-jdk AS ci
WORKDIR /app

COPY gradlew gradlew
COPY gradle gradle
COPY build.gradle settings.gradle ./
COPY src src

RUN chmod +x gradlew
RUN ./gradlew clean test --no-daemon

FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

COPY gradlew gradlew
COPY gradle gradle
COPY build.gradle settings.gradle ./
COPY src src

RUN chmod +x gradlew
RUN ./gradlew bootJar --no-daemon

FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

ENV SPRING_PROFILES_ACTIVE=local \
    DB_HOST=host.docker.internal \
    DB_PORT=5432 \
    DB_NAME=ttalkak_db \
    DB_USERNAME=postgres \
    DB_PASSWORD=1234

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
