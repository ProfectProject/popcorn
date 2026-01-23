FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /app

COPY . .

RUN chmod +x ./gradlew && ./gradlew :gateway:bootJar -x test

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

COPY --from=builder /app/gateway/build/libs/*.jar app.jar

EXPOSE 8080
ENV TZ=Asia/Seoul
ENTRYPOINT ["java", "-jar", "app.jar"]